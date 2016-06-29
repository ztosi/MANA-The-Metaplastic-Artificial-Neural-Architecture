%% Finds the probability that a connection exists between two nodes as a function
% of the number of common in, out, and in and out neighbors they possess and compares
% the results to a null model, optionally plotting the results.
%
%   If two nodes connect to the same node(s) they have that number of "out neighbors",
% alternatively if two nodes each receive some number of connection(s) from the same 
% they are said to have that number of "in neighbors". If two nodes connect to and/or
% from some pool of nodes, they are said to have a number of "neighbors" equal to
% the number of nodes in that pool. This function calculates the number of each
% kind of neighbors between all pairs of nodes in the network and uses that to 
% calculate the probability that any two nodes will be connected to each other
% based on how many common neighbors of each type they share.
%
% DEPENDENTS:
%   dir_generate_srand: For generating directed degree preserving rewirings
%       AUTHOR: Sergei Maslov: Brookhaven National Laboratory
%
%   semistd: finds the lower and upper standard deviations for some set of data,
%   here for results the analysis on the null models
%       AUTHOR: Abdulaziz Alhouti: George Washington University
%
%   errbar: Creates error bars without the "T"s on the ends
%       AUTHOR: Chad Greene
%
%   cN: calculates the number of common neighbors of each pair of nodes
%       AUTHOR: Zach Tosi: Indiana University
%
% COMPATIBILITY: Not tested below R2015b, but likely compatible with earlier versions
%
% EXAMPLE: [pN, modMean, nullM, stdnL, stdnU ] = commonNeigh( adjMat, varargin )
%
% INPUTS:
%       adjMat: the adjacency matrix to be tested w.r.t. dependence of connection
%   upon # of common neighbors. Must be square.
%
% OPTIONAL INPUTS:
%       (adjMat, RW, ...): the number of null models to generate for the purpose of
%   averaging their results. This behavior occurs when the 2nd argument is a
%   scalar.
%       (adjMat, nullMods, ...): a 3D matrix where each entry along the 3rd dimension
%   is a matrix of the same dimension as adjMat. Each of these entries is treated as
%   a null model, the common neighbor analysis of which will be averaged. In this way
%   a user can supply their own null models and avoid computing null models repetitively
%   between analyses which require them.
%        (adjMat, nullMods/RW, showFig): 1 or 0, will cause the function to plot the
%   results of the analysis to a figure or not respectively (0 by default).
%
% OUTPUTS:
%       pN: a vector containing 0:max(# of possible neighbors), assumes no self connections.
%       modMean: a matrix of 3 rows where each column is the probability that a connection
%   exists in adjMat between nodes with pN(col) number of neighbors. The first row is raw
%   neighbors, followed by out and then in neighbors.
%       nullM: same as modMean except that each entry is the mean of the analysis on the null
%   models.
%   stdnL & stdnU: the lower and upper standard deviations for the 
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
function [pN, modMean, nullM, stdnL, stdnU ] = commonNeigh( adjMat, varargin )
    narginchk(1,3);
    nullMods = [];
    RW = 100; % default
    [m, n] = size(adjMat);
    showFig = 0; % don't show a figure by default
    adjMat = uint8(adjMat~=0);
    if m~=n
        error('Input matrix is not square');
    end
    if length(varargin) >= 1
        if isscalar(varargin{1})
            RW = varargin{1};
        else
            nullMods = varargin{1};
            nullMods = uint8(nullMods~=0);
            dims = size(nullMods);
            RW = dims(3);
            if dims(1:2) ~= [m ,n]
                error('Null model have the same dimensions as the adjacency matrix.');
            end
        end
    end

    if length(varargin) == 2
        showFig = varargin{2};
        if showFig > 1 || showFig < 0
            error('0 or 1 are only acceptable inputs for showFig');
        end
    end
    
    %% Calculate common neighbor statistics for the original matrix
    [neigh, neighO, neighI] = cN(adjMat);
    ne= zeros(1, m);
    neO = zeros(1, m);
    neI = zeros(1, m);
    am = triu((adjMat+adjMat')~=0, 1);
    pN = 0:(m-1);
    for k = 1:m         
        ne(1, k) = sum(sum((neigh==k-1) .* am))/ ...
            sum(sum((neigh == k-1)));
        neO(1, k) = sum(sum((neighO==k-1) .* am))/ ...
            sum(sum((neighO == k-1)));
        neI(1, k) = sum(sum((neighI==k-1) .* am))/ ...
            sum(sum((neighI == k-1)));
    end
    
    neighRw = zeros(RW, m);
    neighORw = zeros(RW, m);
    neighIRw = zeros(RW, m);

    % If no null models were supplied generate them by using directed
    % degree-preserving rewiring.
    if isempty(nullMods)
        nullMods = zeros(m,n,RW,'uint8');
        parfor j = 1:RW
            nullMods(:,:,j) = uint8(dir_generate_srand(adjMat));
        end
    end
    
    parfor j = 1:RW
        rwMat = nullMods(:,:,j);
        [rw, rwO, rwI] = cN(rwMat);
        rwMat = triu((rwMat + rwMat')~=0, 1);
        for k = 1:m         
            neighRw(j, k) = sum(sum((rw==k-1) .* rwMat))/ ...
                sum(sum((rw == k-1)));
            neighORw(j, k) = sum(sum((rwO==k-1) .* rwMat))/ ...
                sum(sum((rwO == k-1)));
            neighIRw(j, k) = sum(sum((rwI==k-1) .* rwMat))/ ...
                sum(sum((rwI == k-1)));
            if isnan(neighRw(j, k))
               neighRw(j, k) = 0; 
            end
            if isnan(neighORw(j, k))
               neighORw(j, k) = 0; 
            end
            if isnan(neighIRw(j, k))
               neighIRw(j, k) = 0; 
            end
        end
        
    end
    modMean = zeros(3, length(ne));
    nullM = zeros(3, length(ne));
    stdnL = zeros(3, length(ne));
    stdnU = zeros(3, length(ne));
    if showFig == 1, figure, end;
    for k = 1:3
        if k==1
            nrw = neighRw;
            n = ne;
            modMean(k,:) = ne;
            col = 'k';
        elseif k==2
            nrw = neighORw;
            n = neO;
            modMean(k,:) = neO;
            col = 'b';
        else
            nrw = neighIRw;
            n = neI;
            modMean(k,:) = neI;
            col = 'r';
        end
        [stnL, stnU] = semistd(nrw);
        stdnL(k,:) = stnL;
        stdnU(k,:) = stnU;
        mn = mean(nrw);
        nullM(k,:) = mn;
        if showFig == 1
            subplot(3,1,k); hold on;
            nnans = ~isnan(stnL .* stnU .*mn);
            nnans = nnans & n > 0;
            errbar(pN(nnans), mn(nnans), stnL(nnans), stnU(nnans), col);
            scatter(pN(nnans), mn(nnans), col);
            plot(pN(nnans), n(nnans), col);
            hold off;
        end
    end
end

