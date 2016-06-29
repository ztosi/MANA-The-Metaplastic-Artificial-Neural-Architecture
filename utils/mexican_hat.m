function [ mh ] = mexican_hat( x, sigma, a )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

    mh = a * (2/(sqrt(3*sigma)*pi^(0.25))) .* (1 - ((x.^2)/(sigma^2))) ...
        .* exp((-x.^2)/(2*sigma^2));

end

