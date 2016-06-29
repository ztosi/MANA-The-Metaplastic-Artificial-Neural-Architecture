function [inDeg, avgOutOfIns, stdOutOfIns] ...
    = mean_std_out_of_ins( wts, showPlots )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here

    %avgOutOfIns = zeros(1, length(wts));
    %stdOutOfIns = zeros(1, length(wts));
        binaryWts = wts ~= 0;
     inDeg = sum(binaryWts);
    outDeg = sum(binaryWts,2);
    x = bsxfun(@times, binaryWts, outDeg');
    [avgOutOfIns, stdOutOfIns, ~, ~] = statsNZ(x);
    
    

%     for j = 1:length(wts')
%         oOnly = sum(binaryWts(wts(:, j) ~= 0, :), 2);
%         if (numel(oOnly) ~= 0)
%             avgOutOfIns(1, j) = mean(oOnly);
%             stdOutOfIns(1, j) = std(oOnly);
%         else
%            avgOutOfIns(1, j) = 0;
%            stdOutOfIns(1, j ) = 0;
%         end
%     end
    %avgOutOfIns = avgOutOfIns ./ inDeg;
    %avgOutOfIns(isnan(avgOutOfIns)) = 0;
    if (showPlots == 1)
        figure; scatter(inDeg, avgOutOfIns);  
        figure; scatter(inDeg, stdOutOfIns, 'k');
    end
    %inDegEx = nonzeros(inDeg .* ei);
    %avgOutOfInsEx = nonzeros(avgOutOfIns .* ei);
    %figure;
    %scatter(inDegEx, avgOutOfInsEx, 'r');
    %hold;
    %inDegIn = nonzeros(inDeg .* ~ei);
    %avgOutOfInsIn = nonzeros(avgOutOfIns .* ~ei);
    %scatter(inDegIn, avgOutOfInsIn, 'b');
    

end

