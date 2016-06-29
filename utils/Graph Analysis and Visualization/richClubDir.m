%% Calculates the rich-club coefficient, normalized rich-club coefficient, and the
% significance of the normalized rich-club coefficient for a directed 
% adjacency matrix,using the specified parameter. Tests to see if nodes 
% with a given parameter tend to connect to other nodes with that parameter
% or higher beyond what would be expected by chance. rc_dir calculates the
% actual rich-club coefficient, this function averages over the null models
% and optionally plots the results. The entered matrix is assumed to be
% square and representative of a directed graph, though an undirected graph
% should make no difference. This method can check in and out rich clubs,
% which goes beyond whether or not in/out degree are used as the richness
% parameter, and instead counts how many incoming/outgoing connections
% exist out of how many are possible. 
%
% IMPORTANT: Given adjaceny matrix expected to be such that a nonzero
% element at row i and column j represents a connection FROM i TO j. 
%
% EXAMPLE:
%
% Let's say the resulting matrix when we consider only nodex with riParam >
% V yields the following matrix:
%
%  | X 1 0 |
%  | 0 X 1 |
%  | 1 0 X |
%
%  For riParam > V:
%       Both RCC: 3/6 = 0.5
%       Out RCC: 2/3 = 0.666...
%       In RCC: 1/3 = 0.333...
%
% CITATION:
%   V. Colizza, A. Flamini, M. Serano, and A. Vespignani,
%       "Detecting rich-club ordering in complex networks" Nature physics,
%       vol. 2, no. 2, pp. 110-115,2006
%
% DEPENDENTS:
%
%   dir_generate_srand: 
%       For generating directed degree preserving rewirings
%       AUTHOR: Sergei Maslov: Brookhaven National Laboratory
%
%   semistd: 
%       finds the lower and upper standard deviations for some set of data,
%       here for results the analysis on the null models
%       AUTHOR: Abdulaziz Alhouti: George Washington University
%
%   shadedErrorBar:
%       Uses shaded area to express standard error instead of errorbars for
%       null model means & std devs.
%       AUTHOR:  Rob Campbell: Basel University
%
%   rc_dir: 
%       Performs the actual calculation for the original matrix
%       and each null model
%       AUTHOR: Zach Tosi: Indiana University
%
%
% COMPATIBILITY: Not tested below R2015b, but likely compatible with earlier versions
%
% EXAMPLE: [ rccs, richness, significance ] = richClubDir( adjMat, riParam, varargin )
%
% INPUTS:
%       adjMat: The adjacency matrix of a directed graph upon which the analysis is
%    performed. Must be square. To save memory if a weighted matrix is entered it is
%    transformed into an adjacency matrix of unsigned 8 bit integers.
%       riParam: A vector representing some value associated with each node. The rich-club
%   analysis will determine if nodes of a given value tend to be more or less connected to
%   nodes with a greater or equal value than would be expected by chance. This could be a
%   property associated with each node which is contained in the adjacency matrix like degree,
%   or a completely different value associated with each node from any context.
%
% OPTIONAL INPUTS:
%       (adjMat, riParam, NUM_NULL, ...): the number of null models to generate for the purpose of
%   averaging their results. This behavior occurs when the 2nd argument is a
%   scalar.
%       (adjMat, riParam, nullMods, ...): a 3D matrix where each entry along the 3rd dimension
%   is a matrix of the same dimension as adjMat. Each of these entries is treated as
%   a null model, from which the results of the analysis will be averaged. In this way
%   a user can supply their own null models and avoid computing null models repetitively
%   between analyses which require them. Occurs automatically if the 2nd argument is a 3D matrix.
%       (adjMat, riParam, NUM_NULL/nullMods, type) Because this is a
%       directed rich club coefficient, you are allowed to check if nodes
%       are rich in terms of their in/out/both connections. Acceptable
%       options here are 'in', 'out', or 'both'. Choosing in or out will
%       cause the algorithm to check how many in/out connections exist
%       between all nodes of a given richness parameter or higher out of
%       possible in/out connections which could exist (lower/upper
%       triangles respectively).
%       (adjMat, riParam, NUM_NULL/nullMods, type, showFigs): 1 or 0, shows the results in figures or not, respectively.
% OUTPUTS:
%   nrm_rccs: The normalized rich-club coefficients
%   rccs: the rich-club coefficients
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
function [ nrm_rccs, rccs, significance ] = richClubDir( adjMat, riParam, varargin )
    narginchk(2,5);
    [m, n] = size(adjMat);
    type = 'both';
    %% Error Checking
    if m~=n
        error('Input matrix is not square');
    end
    if length(riParam) ~= m
        error('Vector of values for each node being tested for richness must have length equal to # of rows (and columns) of the adjacency matrix.')
    end
    showFigs = 0;
    NUM_NULL = 100;
    nullMods = [];
    adjMat = uint8(adjMat~=0);
    if ~isempty(varargin)
        if isscalar(varargin{1})
            NUM_NULL = varargin{1};
        else
            nullMods = uint8(varargin{1}~=0);
            dims = size(nullMods);
            NUM_NULL = dims(3);
            if dims(1:2) ~= [m ,n]
                error('Null model have the same dimensions as the given matrix.');
            end    
        end
        if length(varargin)>2
            type = varargin{2};
        end
        if length(varargin)>2
            showFigs = varargin{3};
            if showFigs > 1 || showFigs < 0
                error('0 or 1 are only acceptable inputs for showFigs');
            end
        end
    end
    %% Calculate Rccs for each unique riParam, for the original matrix
    [n, ~] = size(adjMat);
    maxVal = max(riParam);
    [nrm_rccs] = rc_dir(adjMat, riParam, type); % Perform Rich-Club exam on original
    rccs = nrm_rccs;

    %% Now for the null models
    % If no null models were supplied generate them...
    if isempty(nullMods)
        nullMods = zeros(m,n, NUM_NULL, 'uint8');
        parfor i = 1:NUM_NULL
            nullMods(:,:,i) = uint8(dir_generate_srand(adjMat));
        end
    end
    % Calculate the Rccs for the null models
    rccShuff = zeros(NUM_NULL,n);
    parfor i = 1:NUM_NULL
        [rccShuff(i, :)] = rc_dir(nullMods(:,:,i), riParam, type);
    end
    
    %% Find out how significant the results are and determine the normalized Rccs from the null models
    % This is the order the Rccs are return in, so we need to sort this to
    % make graphs
    sortedRIP = sort(riParam, 'ascend');       
    meanRccSh = mean(rccShuff);
    [stdRccShL, stdRccShU] = semistd(rccShuff);
    gr = rccs >= meanRccSh;
    le = rccs < meanRccSh;
    significance = zeros(size(rccs));
    significance(gr) = abs(rccs(gr) - meanRccSh(gr)) ./ stdRccShU(gr);
    significance(le) = abs(rccs(le) - meanRccSh(le)) ./ stdRccShL(le);
    significance = normcdf(-significance, 0, 1);
    significance(isnan(significance)) = 1; % Means there was 0 standard dev
    nrm_rccs = nrm_rccs ./ meanRccSh;
    
    %% Optionally plot the results, showing where the results are significant
    if showFigs == 1
        mx = 1:uint32(maxVal/20):uint32(maxVal+maxVal/10);
        PVAL = .001;
        sig = find(significance < PVAL);
        if sig(end) ~= length(significance)
            sig = [sig length(significance)];
        end
                
        inds = diff(sig);
        %inds
        if sum(inds>1) == 0
            inds = [1 length(sig)];
        else
            if inds(1) == 1
                inds = [0 find(inds>1)];
            else
                inds = find(inds>1);
            end
        end
        %inds
        pairs = zeros(length(inds)-1, 2);
        for i = 1:(length(inds)-1)
            pairs(i,:) = [sortedRIP(sig(inds(i)+1)) ...
                sortedRIP(sig(inds(i+1)))];
        end
        pairs
        maxn = max(nrm_rccs);
        X = zeros(4, length(inds)-1);
        Y = zeros(4, length(inds)-1);
        Y(2,:) = maxn + maxn/10;
        Y(3,:) = maxn + maxn/10;
        for i = 1:length(pairs(:,1))
           X(1, i) = pairs(i, 1);
           X(3, i) = pairs(i, 2);
           X(2, i) = pairs(i, 1);
           X(4, i) = pairs(i, 2);
        end
        if ~ishold % Create a figure and hold, unless we're already plotting to a held figure
            figure;
            hold on;
        end
        patchSaturation=0.15;
        faceAlpha=patchSaturation;
        patchColor=[1, .84, 0];
        set(gcf,'renderer','openGL'); 
        patch(X, Y, patchColor,...
              'edgecolor','none',...
              'facealpha',faceAlpha);
        dummy = patch([0 0 0 0], [0 0 0 0], [1 .97 .75]);
        rc = plot(sortedRIP, rccs, 'r', 'LineWidth', 2);
        seb = shadedErrorBar(sortedRIP, ...
                meanRccSh, ...
                 [stdRccShU; stdRccShL], {'Color', [0, .45, .74], ...
                 'LineWidth', 2}, 1);
        nrc = plot(sortedRIP, nrm_rccs, 'k', 'LineWidth', 2);
        plot(mx, ones(1, numel(mx)), '--k', 'LineWidth', 2);
        xlim([0 maxVal+maxVal/20]);
        ylim([0 maxn+maxn/20]);
        legend([nrc rc seb.mainLine dummy], [type '-\phi_{norm}'],...
            [type '-\phi'], [type '-\phi_{null}'], ['p < ' num2str(PVAL)], ...
            'Location', 'northwest');
        hold off;
    end
    
end

