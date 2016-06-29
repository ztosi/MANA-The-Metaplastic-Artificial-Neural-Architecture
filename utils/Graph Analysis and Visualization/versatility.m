function [ inVers, outVers, vers ] = versatility( wtMat, vArg )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    [m, ~] = size(wtMat);
    adj = wtMat ~= 0;
    inVers = zeros(1, m);
    outVers = zeros(1, m);
    vers = zeros(1, m);
    for i = 1:m
       inVers(i) = std(vArg(i) - vArg(adj(:, i)));
       outVers(i) = std(vArg(i) - vArg(adj(i, :)));
       vers(i) = std(vArg(i) - [vArg(adj(i, :)), vArg(adj(:, i))]);
    end

end

