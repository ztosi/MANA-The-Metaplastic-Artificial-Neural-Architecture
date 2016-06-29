function [alpha, xmin] = powerLawFitAndFig( data, varargin)
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    data=nonzeros(data);
    if ~isempty(varargin)
        ax = varargin{1};
    else
        figure;
        ax = gca;
    end
    
    [alpha, xmin, ll] = plfit(data, 'range', [1.001:0.001:3]);
    %logspace(log10(min(data)), log10(max(data)), 100)
    %[y, BinEdge] = histcounts(data, logspace(log10(min(data(data>0))), log10(max(data)), 200), 'Normalization','pdf');
    [y, BinEdge] = histcounts(data, 'BinMethod', 'integers', 'Normalization', 'pdf');
    
    %[y, BinEdge] = histcounts(data, 'Normalization','pdf');
    x = BinEdge(1:(length(BinEdge)-1)) + diff(BinEdge)/2;
    % Set up fittype and options.
    %ft = fittype( 'power1' );
    %opts = fitoptions( 'Method', 'NonlinearLeastSquares' );
    %opts.Algorithm = 'Levenberg-Marquardt';
    %opts.Display = 'Off';
    %opts.StartPoint = [1 -2];
    %opts.Upper = [4 -3/2];
    %opts.Lower = [0 -3];
    % Fit model to data.
    %[f, g] = fit( x', y', ft, opts );
    %pd = fitdist(data, 'gamma');
    %f
    %g
    hold on;
    set(gca, 'XScale', 'log', 'YScale', 'log');
    scatter(x, y);
    %y = (BinEdge(2)-BinEdge(1)) .* y ./ diff(10.^BinEdge);
    %plot(ax, 10.^x(y>0), y(y>0), 'LineWidth', 2);
    %plot(x, pdf(pd, x), 'LineWidth', 2);
    curveBins = logspace(min(x), max(x), 50);
%     plot(curveBins, f.a.*curveBins.^f.b, ...
%         'k--', 'LineWidth', 2);
    plot(ax, x, x.^-alpha, ...
        'k--', 'LineWidth', 2);
    hold off;
end
