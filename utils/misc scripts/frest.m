function [ estFRs, windFR ] = frest( trains, tauAs, dt )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    
    estFRs = zeros(size(trains));
    for i = 1:length(trains(:,1))
       estFRs(i,:) = estfr(trains(i,:), tauAs(i), dt); 
    end
    estFRs = estFRs .* 1000;
    dt = dt/1000;
    time = length(trains(1,:))*dt;
    windFR = bsxfun(@rdivide, cumsum(trains,2), dt:dt:time); 
    function [efrArr] = estfr(train, tauA, dt)
        t = (0:dt:(10*tauA));
        kern = exp(-t./tauA);
        k = conv(single(train), kern, 'full');
        k = k(1:(end-(length(t)-1)));
        size(k)
        size(train)
        efrArr = zeros(size(train));
        for j = 2:length(train)
            efrArr(j) = efrArr(j-1) + dt * (-efrArr(j-1) + (k(j-1)/tauA));
        end
    end

end

