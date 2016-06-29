function [ parEst, parCI, pf_bin_pow ] = fitPowerBinInterp( dat )
%UNTITLED3 Summary of this function goes here
%   Detailed explanation goes here
    dat = nonzeros(dat)' + 1;
    [b, e] = histcounts(dat, logspace(0, log10(max(dat)), 100 ) );
    [~, I] = max(b);
    mo = (e(I+1)+e(I))/2;
    pf_bin_pow = @(x, q, alpha) ((1-q).*(1./(mo.*((x).^.5)))) ...
        + (q.*(pdf('bino', x, length(x), mo/length(x))));   
    start = [0];
    low = [0];
    high =[1];
    try
    [parEst, parCI] = mle(dat, 'pdf', pf_bin_pow, 'start', start, 'lower', low, 'upper', high);
    catch ME
        parEst = zeros(1,1);
        parCI = zeros(1,1);
    end
    
    
end

