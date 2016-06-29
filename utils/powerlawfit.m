function [ a, b, g ] = powerlawfit(x)
    % y = a x^b 
    % Set up fittype and options.
    x=nonzeros(x);
    x = x-min(x)+1;
    [y, x] = histcounts(x, 'BinMethod', 'fd', 'Normalization', 'pdf');
    x = x(1:(length(x)-1)) + (diff(x)./2);
    any(y==0)
    ft = fittype( 'power1' );
    opts = fitoptions( 'Method', 'NonlinearLeastSquares' );
    %opts.Algorithm = 'Levenberg-Marquardt';
    opts.Display = 'Off';
    % Fit model to data.
    ynz = y>0;
    [f, g] = fit( x(ynz)', y(ynz)', ft, opts );
    a=f.a;
    b=f.b;
end

