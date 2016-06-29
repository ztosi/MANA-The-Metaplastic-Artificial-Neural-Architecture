function [ y ] = LogisticSTDP( x, a, b, c )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
    y = -a*((exp(x/b).*(exp(x/b) - 1).*(1+(x/c).^2))./(exp(x/b)+1).^3);
end

