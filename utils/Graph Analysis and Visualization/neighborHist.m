function [ out, commonNeigh, cNRw, cNRwMn, cNRwSt ] = neighborHist ( wts, varargin )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

%% Setup variables
    if ~isempty(varargin)
        NUM_RW = varargin{1}; 
    else
        NUM_RW = 100;
    end
    binMat = wts ~= 0;
    n = length(binMat(1,:));
    
%% Get number of common neighbors for each connected pair    
    [I, J] = find(binMat ~= 0);
    nn = numel(I);
    out = zeros(1, nn);
    parfor k = 1:nn
        out(k) = sum( ((binMat(I(k), :) .* binMat(J(k), :)) +  ...
            (binMat(:, I(k)) .* binMat(:, J(k)))') ~= 0);
    end
    commonNeigh = hist(out, 0:n);

%% Get number of common neighbors for connected pairs in NUM_RW degree-preserving rewires
    cNRw = zeros(NUM_RW, n+1);
    parfor i = 1:NUM_RW       
        rw = dir_generate_srand(binMat);
        [I, J] = find(rw ~= 0);
        o = zeros(1, nn);
        for k = 1:nn
            o(k) = sum( ((rw(I(k), :) .* rw(J(k), :)) +  ...
                (rw(:, I(k)) .* rw(:, J(k)))') ~= 0);
        end
        cNRw(i, :) = hist(o, 0:n);
    end
    cNRwMn = mean(cNRw);
    cNRwSt = std(cNRw);
    
 %% Plot result   
    
    figure; hold on;
    xs = 0:n;
    nz = commonNeigh > 0;
    %nzf = find(nz);
    cN =  commonNeigh;
    %Actual common neigbors
    %plot(xs(nz), cN, 'b-');
    % Null model common neighbors
    plt = 1:10:n;%intersect(1:10:n, nzf);
    errbar(xs(plt), cNRwMn(plt), cNRwSt(plt), 'k-', ...
        'MarkerSize', 5, 'LineWidth', 1);
    plot(xs(plt), cNRwMn(plt), 'k.-', ...
        'MarkerSize', 5, 'LineWidth', 2);
    
    colors = normcdf(-abs((cN(plt)-cNRwMn(plt))./cNRwSt(plt)), 0, 1);
    colors(colors>0.05) = 0.05;
    colormap parula;
    colormap(flipud(colormap));
    %scatter(xs(nz), cN, 20, colors, 'filled');
    surface([xs(plt);xs(plt)], [cN(plt);cN(plt)], ...
        [zeros(size(cN(plt)));zeros(size(cN(plt)))], ...
        [colors;colors], 'facecol', 'no', 'edgecol', 'interp', 'linew', 2);
    colorbar;
    hold off;

end

