%% Takes in a weight matrix (or adjacency matrix) and optionally a vector, then
% displays a surface plot where histograms for either the individual rows or columns
% of the matrix are displayed simultaneously. In more simple terms
% simultaneously displays histograms for each individual row or column of a
% matrix, using either the weight values in the matrix or the values in the
% supplied vector which are assumed to reflect some property of the nodes
% the graph represented by the matrix represents. Ignores zero-valued
% entries. EXAMPLE: Say the vector contains firing rates. This function
% will produce side by side histograms of the firing rates of the nodes
% which send connections to each node.
%
% DEPENDENTS:
%   
%   None.
%
% COMPATIBILITY: Not compatible with versions earlier than MATLAB 2014b
%
% EXAMPLE: histInOutEdges(wtMatix, varargin)
%
% INPUTS:
%       wtMatrix: A matrix represnting either a weight matrix (directed or
%       otherwise) or an adjacency matrix if a vector of node values are
%       supplied. An adjaceny matrix can be supplied without a vector, but
%       this will not produce a very interesting result... If a weighted
%       matrix is supplied without a vector then the resulting histograms
%       will reflected the weighted entries in each row or column. Bins 
%       applied to each row or column are created from the
%       Friedman-Diaconis rule applied to all nonzero entries in the
%       matrix.
%
% OPTIONAL INPUTS:
%       
%       (wtMat, 'Sort by', srtVec): In order for the many side by side
%       histograms to be intelligible it's advisable to order the nodes in
%       the network by some value. By default the sum of absolute weight
%       values in each row or column is used to order the nodes. Should
%       usually be the same as "useVec" if that is supplied, but this
%       doesn't have to be the case.
%       
%       (wtMat, 'Direction', dir): where dir is either 'incoming' or
%       'outgoing'. If 'incoming' then hisograms are derived from column
%       entries (or columns of the adjacency matrix element-wise multiplied
%       by the supplied vector) and if 'outgoing' then histograms are
%       derived from row entries. Thus the direction of the supplied weight
%       matrix is assumed to be: Entry at row i, column j represents the
%       edge FROM node i TO node j. DEFAULT: 'incoming'
%
%       (wtMat, 'Use', useVec): If this vector is supplied then the wtMat
%       is converted to an adjacency matrix if not already. Then depending
%       on "dir" will populate nonzero row or column entries using these
%       values. The result is that the values in useVec are assumed to be
%       values at each vertex and the resulting histograms display the
%       distribution of these values for each nodes that each node connects
%       to or receives connections from.
%
% OUTPUTS:
%      None
%%   
% AUTHORS: Zach Tosi
% EMAIL: ztosi@indiana.edu
% May 2016

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
function [hcounts, edges, KL, p_vals, nsamp] = histInOutEdges( wtMat, varargin )
    narginchk(1,7);
    
    if mod(nargin, 2) == 0
        error('Improper number of arguments');
    end
    srtVec = [];
    useVec = [];
    dir = 'incoming';
    for i=1:2:length(varargin)
        switch varargin{i}
            case 'Sort by'
                srtVec = varargin{i+1};
            case 'Direction'
                dir = varargin{i+1};
            case 'Use'
                useVec = varargin{i+1};
            otherwise
                error('Unknown input.');
        end
    end

    if strcmp(dir, 'incoming')
        % Do nothing (assumes row i, col j means from i to j
    elseif strcmp(dir, 'outgoing')
        wtMat = wtMat';
    else
       error('Unknown option'); 
    end
    if ~isempty(useVec)
       if  isrow(useVec) useVec = useVec'; end
       wtMat = wtMat ~= 0; % Converto to adjacency
       wtMat = bsxfun(@times, useVec, wtMat);
    else 
       useVec = nonzeros(wtMat); 
    end
    if isempty(srtVec)
        srtVec = sum(abs(wtMat));
    end
    
    allInts = isequal(useVec, floor(useVec));
    if allInts
        binMeth = 'integers';
    else
        binMeth = 'fd';
    end
    
    [~, n] = size(wtMat);  
    [netPdf, edges] = histcounts(nonzeros(wtMat), 'BinMethod', binMeth,...
        'Normalization', 'probability');
    bins = edges(1:(end-1)) + (diff(edges)/2);
    binSize = edges(2)-edges(1);
    
    KL = zeros(1,n);
    p_vals = zeros(1,n);
    nsamp = zeros(1,n);
    
    [~, inds] = sort(srtVec); 
    [X, Y] = meshgrid([bins(1)-binSize bins bins(end)+binSize], 0:0.25:n+0.5);
    Z = zeros(size(X));
    C = zeros(size(X));
    
    for i = 1:4:4*n
       currInd = inds((i+3)/4);
       cnts = histcounts(nonzeros(wtMat(:,currInd)), edges); 
       
       locPdf = cnts ./ (sum(cnts));
       kl = locPdf .*  log2(locPdf./(netPdf));
       entropy = -netPdf .* log2(netPdf);
       entropy = sum(entropy(isfinite(entropy)));
       
       KL(currInd) = 1-(sum(kl(isfinite(kl)))/entropy);
       
       if allInts
           G = 2 * (length(edges)-1) * KL(currInd);
           p_vals(currInd) = chi2cdf(G, length(edges)-1, 'upper');
       else
           tval = nonzeros(wtMat(:, currInd));
           if isempty(tval)
               p_vals(currInd) = 0;
           else
               [~, p_vals(currInd)] = kstest2(nonzeros(wtMat(:,currInd)),...
                   useVec);
           end
       end
       nsamp(currInd) = sum(cnts);
       
       if sum(cnts) == 0
           Z(i,:) = NaN;
           Z(i+1,:) = NaN;
           Z(i+2,:) = NaN;
           Z(i+3,:) = NaN;
           C(i,:) = NaN;
           C(i+1,:) = NaN;
           C(i+2,:) = NaN;
           C(i+3,:) = NaN;
           continue;
       end
       nzinds = find(cnts > 0);
       Z(i,:) = 0;
       Z(i+3,:) = 0;
       Z(i+1,nzinds) = cnts(nzinds);
       Z(i+2,nzinds) = cnts(nzinds);
       for j = 0:3
           Z(i+j, (max(nzinds)+2):length(Z(i,:))) = NaN;
           Z(i+j, 1:(min(nzinds)-2)) = NaN;
           C(i+j,:) = Z(i+1,:);
       end
       
    end
    
    hcounts = Z(2:4:4*n,:);
    hcounts(isnan(hcounts)) = 0;
    
    figure;
    surf(X,Y,Z,C); %'EdgeColor', 'none');
    shading interp;
    ylim([0.25 n+.25]);
    xlim([min(min(X)) max(max(X))]);

end

