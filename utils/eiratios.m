function [ p, ee, ei, ie, ii ] = eiratios( wtMat, ei )
%UNTITLED3 Summary of this function goes here
%   Detailed explanation goes here
    [m, n] = size(wtMat);
    bin = wtMat ~= 0;
    p = sum(sum(bin)) / (m * (n-1));
    exin = ei;%= any(wtMat>0,2);
    
    in = ~ei; %any(wtMat<0,2);
    if sum(exin & in) > 0
        error('Neurons have negative and positive outs!');
    end

    ee = sum(sum(bin(exin, exin))) / (sum(exin)*(sum(exin)-1));
    ei = sum(sum(bin(exin, ~exin))) / (sum(exin) * sum(~exin));
    ie = sum(sum(bin(~exin, exin))) / (sum(exin) * sum(~exin));
    ii = sum(sum(bin(~exin, ~exin))) / (sum(~exin) * (sum(~exin)-1));

end

