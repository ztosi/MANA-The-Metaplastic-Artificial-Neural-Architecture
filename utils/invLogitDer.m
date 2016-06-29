function [ y ] = invLogitDer( x )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    y = -4*(exp(x./4)./((exp(x./4) + 1).^2)) + 1;

end

