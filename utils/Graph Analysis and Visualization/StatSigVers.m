%% Calculates the versatility of nodes in a network with respect to an aribitrary set
% of values assigned to each node. AKA the standard deviation of the differences between
% the value posessed by a node and the values posessed by its neighbors. Low for nodes
% connected to many other nodes which all have similar values, and high for nodes connected
% to other nodes which have a wide range of the value in question. This function doesn't
% do this calculation itself, instead calling nodeVersatility. This function compares
% the versatility values of the nodes in the network in question to some number of
% null models and returns the versatility values for the network, the 1st and 2nd moments
% of the versatility values for the null models, and the significance of the versatility
% of each node in the network based on this. This function also optionally scatters the
% versatility values against the versatility parameter, and displays a histogram of 
% the versatility values.
%
% CITATION:
%   G. Thedchanamoorthy, M. Piraveenan, and D. Kasthurirathna, "Standard deviations of
%       degree differences as indicators of mixing patters in complex networks,"
%       IEEE/ACM International Conference on Advances in Social Networks Analysis
%       and Mining (ASONAM), pp 1202-1208, 2013.   
%
%
% DEPENDENTS:
%   
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
%   nodeVersatility: Performs the actual calculation for the original matrix
%   and each null model
%       AUTHOR: Zach Tosi: Indiana University
%
%
% COMPATIBILITY: R2015b or later
%
% EXAMPLE: [ vers, versPVal, nullMV, nullStVL, nullStVU ] = StatSigVers( adjMat, versVar, varargin )
%
% INPUTS:
%       adjMat: The adjacency matrix of a directed graph upon which the analysis is
%    performed. Must be square. To save memory if a weighted matrix is entered it is
%    transformed into an adjacency matrix of unsigned 8 bit integers.
%       versVar: A vector representing some value associated with each node. Versatility is calculated
%    for each node with respect to this value, which can be degree or any arbitrary parameter associated
%    with each node.
%
% OPTIONAL INPUTS:
%       (adjMat, versVar, NUM_NULL, ...): the number of null models to generate for the purpose of
%   averaging their results. This behavior occurs when the 2nd argument is a
%   scalar.
%       (adjMat, versVar, nullMods, ...): a 3D matrix where each entry along the 3rd dimension
%   is a matrix of the same dimension as adjMat. Each of these entries is treated as
%   a null model, from which the results of the analysis will be averaged. In this way
%   a user can supply their own null models and avoid computing null models repetitively
%   between analyses which require them. Occurs automatically if the 2nd argument is a 3D matrix.
%       (wts, NUM_NULL/nullMods, sigP, ...): the p-value cutoff for significance. Only important if 
%   we are displaying a figure, as it prevents super small p-values from being obscured by large ones.
%       (adjMat, versVar, NUM_NULL/nullMods, sigP, showFigs): 1 or 0, shows the results in figures or not, respectively.
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
function [ vers, versPVal, nullMV, nullStVL, nullStVU ] = StatSigVers( ...
    adjMat, versVar, varargin )
    narginchk(2, 5);
    [m, n] = size(adjMat);
    adjMat = uint8(adjMat ~= 0);
    if m~=n
        error('Input matrix is not square');
    end
    if length(versVar) ~= m
        error('Vector of versatility variables must have length equal to # of rows (and columns) of the adjacency matrix.')
    end
    NUM_NULL = 100;
    sigP = .1;
    showFigs = 0;
    nullMods = [];
    if nargin >= 3
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
    end
    if nargin >= 4
        sigP = varargin{2};
    end
    if nargin == 5
        showFigs = varargin{3};
    end

    if isempty(nullMods)
        nullMods = zeros(m,n,NUM_NULL,'uint8');
        parfor i = 1:NUM_NULL
            nullMods(:,:,i) = uint8(dir_generate_srand(adjMat));
        end
    end

    n = numel(versVar);
    nullMV = zeros(NUM_NULL, n);
    parfor i = 1:NUM_NULL
       nullMV(i, :) = nodeVersatility(nullMods(:,:,i), versVar, 0); 
    end
    [nullStVL, nullStVU] = semistd(nullMV);
    nullMV = mean(nullMV);
    vers =  nodeVersatility(adjMat, versVar, 0);
    le = vers < nullMV;
    ge = vers >= nullMV;
    versPVal = zeros(1, n);
    versPVal(le) = (nullMV(le) - vers(le)) ./ nullStVL(le);
    versPVal(ge) = (vers(ge) - nullMV(ge)) ./ nullStVU(ge);
    versPVal = normcdf(-versPVal, 0, 1); % Turning z-scores into p-values

    if showFigs == 1
        figure;
        subplot(1, 2, 1);
        hold on;
        colormap parula;
        colormap(flipud(colormap));
        a = 50;
        b = 15;
        scatter(versVar, nullMV, b, 'ko');
        errbar(versVar, nullMV, nullStVL, nullStVU, 'k-');  
        c = versPVal;
        c(c>sigP) = sigP;
        scatter(versVar, vers, a, c, 'filled');
        h = colorbar;
        ylabel(h, 'p-value');
        %scatter(versVar(ei), vers(ei), a, [.8 .1 .1]);
        %scatter(versVar(~ei), vers(~ei), a, [0 .1 .8]);
        hold off;
        subplot(1, 2, 2);
        [~, edges] = histcounts(vers, 'Normalization', 'pdf',  ...
            'BinMethod', 'fd');
        hold on;
        hv = histogram(vers, edges, 'Normalization', 'pdf');
        set(hv, 'FaceAlpha', .5);
        set(hv, 'FaceColor', [.1 .4 .6]);
        set(hv, 'EdgeAlpha', 0);
        hv = histogram(nullMV, edges, 'Normalization', 'pdf');
        set(hv, 'FaceAlpha', .5);
        set(hv, 'FaceColor', [0 0 0]);
        set(hv, 'EdgeAlpha', 0);
        
    end

end

