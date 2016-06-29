function [ stateMat ] = asdf2state_mat( asdf, timeWindow, ...
    collapseMethod, tau )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
    
    numNeurons = asdf{length(asdf)}(1);
    numNeurons
    rows = uint32(asdf{length(asdf)-1}(1) * asdf{length(asdf)}(2)...
        / timeWindow);
    rows
    stateCell = cell(numNeurons, 1);
    for i = 1:numNeurons
       stateCell{i} = zeros(rows, 1);
    end
    length(stateCell)
    length(asdf(1:numNeurons))
    
    indArr = [];
    times = [];
    
    for i = 1:numNeurons
        
        
    end
    
    
    if(strcmp(collapseMethod, 'convolution'))
        stateCell = cellfun(@expConv, asdf(1:numNeurons), ...
            stateCell, 'UniformOutput', false); 
    elseif (strcmp(collapseMethod, 'bin'))
        bins = 1:timeWindow:asdf{length(asdf)}(2)*asdf{length(asdf)-1}(1);
         parfor i=1:numNeurons
             [freq, ~] = hist(asdf{i}, bins);
             stateCell{i} = uint8(freq);
         end
    end
    
    function [matCol] = expConv(times, matCol)
        if ~isempty(times)
            n = length(matCol);
            parfor k = 1:n
               matCol(k) = sum(exp(((times .* (times < (k * timeWindow))) ...
                   - (k * timeWindow))/tau)); 
            end
        end
    end
    
    stateMat = zeros(rows, numNeurons, 'uint8');
    for i = 1:numNeurons
       stateMat(:, i) = stateCell{i}; 
    end


end

