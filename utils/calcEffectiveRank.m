function [ effRank ] = calcEffectiveRank( stateMat )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

    sings = svd(stateMat);
    i = 0;
    c = 0;
    while c < 0.99 * sum(sings)
       i = i + 1;
       c = c + sings(i);
    end
    effRank = i;
end

