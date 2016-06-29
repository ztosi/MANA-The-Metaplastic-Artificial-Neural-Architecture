function [ lines, tr ] = synapseFFT( synCh, sampFreq )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    synCh = abs(synCh);
    [noSyns, time] = size(synCh);
    ps = zeros(noSyns, 2);
    for i = 1:noSyns
        ps(i, :) = polyfit(1:time, synCh(i, :), 1);
    end
    lines = (repmat(1:time, noSyns, 1) .* repmat(ps(:,1), 1, time))...
        + repmat(ps(:,2), 1, time);
    
    tr = repmat(lines(:, time), 1, time) - lines;
    
    tr = (synCh + tr) - repmat(lines(:, time), 1, time);
    
    figure; plot(tr');
    
    NFFT = 2^nextpow2(time);
    four = fft(tr', NFFT)/time;
    f = sampFreq/2*linspace(0,1,NFFT/2+1);
    four = 2*abs(four(1:NFFT/2+1, :));
    [m1, I] = max(four);
    
    for i = 1:noSyns
       four(I(i), i) = 0; 
    end
    
    [m2, I2] = max(four);
    
    holder = m1;
    m1 = m1 ./ (m1+m2);
    m2 = m2 ./ (holder + m2);
    
    endVals = synCh(:, time);
    
    figure; scatter(endVals, m1.*f(I) + m2.*f(I2));

end

