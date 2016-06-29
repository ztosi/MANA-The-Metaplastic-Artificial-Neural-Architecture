function [ y ] = logistic( x, a, b )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
        y = a./(1+exp(-b.*x));

end

