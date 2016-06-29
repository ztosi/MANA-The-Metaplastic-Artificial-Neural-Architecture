function [ collMat ] = collapseSpkTrains( spkTrains, numNeurons, ...
    timeWindow, technique )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    startTime = spkTrains(1, 1);
    collMat = zeros(ceil((spkTrains(length(spkTrains), 1) - spkTrains(1,1)) ...
        / timeWindow), numNeurons);
    [~, col] = size(spkTrains);
    
    t_ind = 1;
    if technique == 1
        while t_ind < length(spkTrains)
           cmInd = ceil(((spkTrains(t_ind, 1)-startTime) + 1) / timeWindow);
           neurInds = nonzeros(spkTrains(t_ind, 2:col));
           collMat(cmInd, neurInds) = collMat(cmInd, neurInds) + 1;
           t_ind = t_ind+1;
        end
        
    elseif technique == 2
        while t_ind < length(spkTrains)
           time = ((spkTrains(t_ind, 1)-startTime) + 1);
           cmInd = ceil(time / timeWindow);
           neurInds = nonzeros(spkTrains(t_ind, 2:col));
           collMat(cmInd, neurInds) = collMat(cmInd, neurInds) ...
               + exp(-(((cmInd * timeWindow) - time))/30);
           t_ind = t_ind+1;
        end
    elseif technique == 3
                
    else
                
    end
            


end

