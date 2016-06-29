function [ out ] = expand( indices, sz )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
    edges = 0.5:1:1000.5;
    out = histcounts(indices(:,1), edges);

end

