function [ z ] = loglogitpdf( x, mu, sigma )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
    
    q = (log(x) - mu) / sigma;
    
    z = (1 / sigma) * (1 ./ x) .* (exp(q) ./ (1 + exp(q)).^2); 

end

