%% For each node in a directed graph finds the mean and standard deviation of the out degree
% of nodes which connect to that node. That is, the average and std of the out-degree of
% progenitors of incoming connections. Does this for degree-preserving random rewires as null
% models for comparison. Optionally plots the results separating out nodes with negative outgoing
% edge weights. Will work for an undirected graph, but was intended for use with directed graphs.
% Gives p-values for each item, and thus its significance as opposed to mean_std_out_of_ins which
% does the actual calculation.
%
%   For a directed graph G(V,E), where each vertex v_i has k_in incoming edges e_ji,
% (j = 1, ..., k_in) connecting v_j to v_i, and thus a set {N} comprised of the out-degrees
% of v_(1, ..., k_in) calculates E[N] and the standard deviation of the members of set N. Does
% this for some number of models to determine if E[N] and/or std[N] are significant for all
% v in V.
%
% DEPENDENTS:
%   
%   dir_generate_srand: For generating directed degree preserving rewirings
%       AUTHOR: Sergei Maslov: Brookhaven National Laboratory
%
%   semistd: finds the lower and upper standard deviations for some set of data,
%   here for results the analysis on the null models
%       AUTHOR:
%
%   errbar: Creates error bars without the "T"s on the ends
%       AUTHOR: Chad Greene
%
%   mean_std_out_of_ins: Performs the actual calculation for the original matrix
%   and each null model
%       AUTHOR: Zach Tosi
%
%
% COMPATIBILITY: Not tested below R2015b, but likely compatible with earlier versions
%
% EXAMPLE: function [avgOIM, stdOIM, nullMeanStats, nullStdStats] = mean_std_out_of_ins( wts, varargin )
%
% INPUTS:
%       wts: A weighted directed graph with positive or negative edge weights upon which the analysis is
%   performed. Must be square.
%
% OPTIONAL INPUTS:
%       (wts, NUM_NULL, ...): the number of null models to generate for the purpose of
%   averaging their results. This behavior occurs when the 2nd argument is a
%   scalar.
%       (adjMat, nullMods, ...): a 3D matrix where each entry along the 3rd dimension
%   is a matrix of the same dimension as adjMat. Each of these entries is treated as
%   a null model, from which the results of the analysis will be averaged. In this way
%   a user can supply their own null models and avoid computing null models repetitively
%   between analyses which require them. Occurs automatically if the 2nd argument is a 3D matrix.
%       (wts, NUM_NULL/nullMods, sigP, ...): the p-value cutoff for significance
%       (wts, NUM_NULL/nullMods, sigP, ei): Tells the function which nodes are excitatory/inhibitory
%   directly instead of relying upon its method for determining that.
%       (wts, NUM_NULL/nullMods, sigP, ei, showFigs): 1 or 0, shows the results in figures or not, respectively.
%
%%   
% AUTHORS: Zach Tosi
% EMAIL: ztosi@indiana.edu
% March 2016

%%==============================================================================
% Copyright (c) 2016, The Trustees of Indiana University
% All rights reserved.
% 
% Redistribution and use in source and binary forms, with or without
% modification, are permitted provided that the following conditions are met:
% 
%   1. Redistributions of source code must retain the above copyright notice,
%      this list of conditions and the following disclaimer.
% 
%   2. Redistributions in binary form must reproduce the above copyright notice,
%      this list of conditions and the following disclaimer in the documentation
%      and/or other materials provided with the distribution.
% 
%   3. Neither the name of Indiana University nor the names of its contributors
%      may be used to endorse or promote products derived from this software
%      without specific prior written permission.
% 
% THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
% AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
% IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
% ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
% LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
% CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
% SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
% INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
% CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
% ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
% POSSIBILITY OF SUCH DAMAGE.
%
function [avgOIM, stdOIM, nullMeanStats, nullStdStats] = sig_mean_std_out_of_ins( wts, varargin )
    narginchk(1,5);
    [m, n] = size(wts);
    if m~=n
        error('Input matrix is not square');
    end
    ei = ~any(wts<0,2); % Find excitatory neurons, assumes zero out degree is excitatory
    sigP = 0.1;
    nullMods = [];
    NUM_NULL = 100; % default
    showFigs = 0;
    if length(varargin) >= 1
        if isscalar(varargin{1})
            NUM_NULL = varargin{1};
        else
            nullMods = varargin{1};
            dims = size(nullMods);
            NUM_NULL = dims(3);
            if dims(1:2) ~= [m ,n]
                error('Null model have the same dimensions as the given matrix.');
            end    
        end
    end

    if length(varargin) == 2
        sigP = varargin{2};
    elseif length(varargin) == 3
        sigP = varargin{2};
        ei = varargin{3};
    elseif length(varargin) == 4
        sigP = varargin{2};
        ei = varargin{3};
        showFigs = varargin{4};
        if showFigs > 1 || showFigs < 0
            error('0 or 1 are only acceptable inputs for showFigs');
        end
    end

    nullMOI = zeros(NUM_NULL, n);
    nullSOI = zeros(NUM_NULL, n);
    if isempty(nullMods)
        nullMods = zeros(m,n,NUM_NULL);
        parfor i = 1:NUM_NULL
            nullMods(:,:,i) = dir_generate_srand(wts);
        end
    end

    [inDeg, nullMOI(1, :), nullSOI(1, :)] = mean_std_out_of_ins(nullMods(:,:, 1), 0);

    parfor i = 2:NUM_NULL
        [~, nullMOI(i, :), nullSOI(i, :)] = mean_std_out_of_ins(nullMods(:,:,i), 0);
    end

    finalNullMoiMean = mean(nullMOI);
    [finalNullMoiStdL, finalNullMoiStdU] = semistd(nullMOI);
    nullMeanStats = [finalNullMoiMean; finalNullMoiStdU; finalNullMoiStdL];
    finalNullSoiMean = mean(nullSOI);
    [finalNullSoiStdL, finalNullSoiStdU] = semistd(nullSOI);
    nullStdStats = [finalNullSoiMean; finalNullSoiStdU; finalNullSoiStdL];

    if showFigs == 1
        figure; hold on;
        a=7;
        b=5;
        handles = [];
        names = {};
        handles(1, 1) = plot(inDeg, finalNullMoiMean, 'ko', 'MarkerSize', b,...
            'LineWidth', 1);
        names{1} = 'Null Model Mean';
        [~, avgOIM, stdOIM] = scatterInDegVsOutOfIns(wts, 0);
        title('Means');
        errbar(inDeg, finalNullMoiMean, finalNullMoiStdL, ...
            finalNullMoiStdU, 'k-');
        colormap hot;
        colormap(flipud(colormap));
        c = zeros(1, NUM_NULL);
        le = avgOIM < finalNullMoiMean;
        ge = avgOIM >= finalNullMoiMean;
        c(le) = normcdf(-abs(finalNullMoiMean(le) - avgOIM(le)) ...
            ./ finalNullMoiStdL(le), 0, 1);
        c(ge) = normcdf(-abs(avgOIM(ge) - finalNullMoiMean(ge)) ...
            ./ finalNullMoiStdU(ge), 0, 1);
        c(c > sigP) = sigP;
        scatter(inDeg, avgOIM, a*5, c, 'filled');
        hind = 2;
        if sum(ei) ~= 0
            handles(1, hind) = plot(inDeg(ei), avgOIM(ei), 'o',  ...
                'MarkerSize', a, 'Color', [.8 .1 .1], 'LineWidth', 1);
            names{hind} = 'Excitatory';
            hind = hind + 1;
        end
        if sum(~ei) ~= 0
            handles(1, hind) = plot(inDeg(~ei), avgOIM(~ei), 'o',  ...
                'MarkerSize', a, 'Color', [0 .1 .8], 'LineWidth', 1);
            names{hind} = 'Inhibitory';
        end
        if length(names) == 2
            l = legend(handles, names{1}, names{2});
        else
            l = legend(handles, names{1}, names{2}, names{3});
        end
        set(l, 'Location', 'best');
        set(l, 'EdgeColor', [1 1 1]);
        h = colorbar;
        ylabel(h, 'p-value');
        hold off;

        figure; hold on;
        handles2 = [];
        names2 = {};
        handles2(1, 1) = plot(inDeg, finalNullSoiMean, 'ko', 'MarkerSize', b ...
            , 'LineWidth', 1);
        names2{1} = 'Null Model Mean';
        title('Std. Devs');
        errbar(inDeg, finalNullSoiMean, finalNullSoiStdL, ...
            finalNullSoiStdU, 'k-');
        colormap hot;
        colormap(flipud(colormap));
        c = zeros(1, NUM_NULL);
        le = stdOIM < finalNullSoiMean;
        ge = stdOIM >= finalNullSoiMean;
        c(le) = normcdf(-abs(finalNullSoiMean(le) - stdOIM(le))  ...
            ./ finalNullSoiStdL(le), 0, 1);
        c(ge) = normcdf(-abs(stdOIM(ge) - finalNullSoiMean(ge)) ...
            ./ finalNullSoiStdU(ge), 0 , 1);
        c(c > sigP) = sigP;
        scatter(inDeg, stdOIM, 5*a, c, 'filled');
        hind = 2;
        if sum(ei) ~= 0
            handles2(1, hind) = plot(inDeg(ei), stdOIM(ei), 'o',  ...
                'MarkerSize', a, 'Color', [.8 .1 .1], 'LineWidth', 1);
            names2{hind} = 'Excitatory';
            hind = hind + 1;
        end
        if sum(~ei) ~= 0
            handles2(1, hind) = plot(inDeg(~ei), stdOIM(~ei), 'o',  ...
                'MarkerSize', a, 'Color', [0 .1 .8], 'LineWidth', 1);
            names2{hind} = 'Inhibitory';
        end
        if length(names2) == 2
            l = legend(handles2, names2{1}, names2{2});
        else
            l = legend(handles2, names2{1}, names2{2}, names2{3});
        end
        set(l, 'Location', 'best');
        set(l, 'EdgeColor', [1 1 1]);
        h = colorbar;
        ylabel(h, 'p-value');
        hold off;
    end
end

