function [ pcVneigh ] = connectProbRelToNeighbors( wts )
%UNTITLED4 Summary of this function goes here
%   Detailed explanation goes here
    [~, s] = size(wts); %must be square
    bin = wts ~= 0;
    nn = zeros(1, s+1);
    nnc = zeros(1, s+1);
    
    for i=1:s
        iCons = bin(i, :) | bin(:, i)';
       for j = i+1:s
           jCons = bin(j, :) | bin(:, j)';
           numN = sum(iCons & jCons);
           if bin(i, j) == 0
              nnc(numN+1) = nnc(numN+1) + 1; 
           else
              nn(numN+1) = nn(numN+1) + 1;
           end
       end
    end
    
    pcVneigh = nn ./ (nn + nnc);
    

end

