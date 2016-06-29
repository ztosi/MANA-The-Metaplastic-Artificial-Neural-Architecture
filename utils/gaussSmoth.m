function [ smoothedVec ] = gaussSmoth( vec, mu, sig, width )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

    N = length(vec);
    window = -width:width;
    blur = normpdf(window, mu, sig);
    nnz = (3*(width*(width+1))/2) + width + 1;
    nnz = nnz * 2;
    nnz = nnz + ((2*width+1)*(N - (4*(width+1))));
    
    I = zeros(1, nnz);
    J = zeros(1, nnz);
    V = zeros(1, nnz);
    l = 1;
    for d = 1:width
        for k = 1:(N-(d))
           I(l) = k+d;
           J(l) = k;
           V(l) =  blur(width + 1 - d);
           l=l+1;
           I(l) = k;
           J(l) = k+d;
           V(l) =  blur(width + 1 + d);
           l=l+1;
       end
    end
    for i = 1:N
        I(l) = i;
        J(l) = i;
        V(l) =  blur(width + 1 - d);
        l=l+1;
    end
    S = sparse(I, J, V, N, N);
    
    smoothedVec = S * vec';

end

