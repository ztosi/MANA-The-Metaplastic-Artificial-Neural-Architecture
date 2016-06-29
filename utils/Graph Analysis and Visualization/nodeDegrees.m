function [ kIn, kOut, k ] = nodeDegrees( mat )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

    kIn = sum(mat ~= 0);
    kOut = sum(mat ~= 0, 2)';
    k = kIn + kOut;

end

