function [ f, P1, Y] = quickFFT( X, Fs, L, plt )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
    T=1/Fs;
    Y = fft(X);
    P2 = abs(Y/L);
    P1 = P2(1:L/2 + 1);
    P1(2:end-1) = 2 * P1(2:end-1);
    f = Fs * (0:(L/2))/L;
    if plt==1
        figure;
        plot(f, P1);
    end
end

