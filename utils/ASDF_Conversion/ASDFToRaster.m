% [raster, binunit] = ASDFToSparse(asdf)
%
%    asdf - {n_neu + 2, 1} ASDF data.
%
% Returns :
%    raster - (n_neu, duration) time raster expressed as a sparse matrix if greater than
%	 1 GB.
%    binunit - (string) the unit of time in the data. (length of a bin in real scale)
%
% Description :
%    This function converts the time raster of ASDF to a raster in the form of a
% 	 dense or sparse matrix depending on its size. If the given (optional)
%    bin size is larger than the bin size of the ASDF file, the resulting 
%    raster will contain the number of spikes in each bin.

%==============================================================================
% Copyright (c) 2016, The Trustees of Indiana University
% All rights reserved.
% 
% Authors:
% Zach Tosi (ztosi@indiana.edu),
% Michael Hansen (mihansen@indiana.edu),
% Shinya Ito (itos@indiana.edu)
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
%==============================================================================

function [raster, binunit] = ASDFToRaster(asdf, varargin)
	tic;
	narginchk(1, 2);	
	info = asdf{end};
	n_neu = info(1);
	duration = info(2);
	binunit = asdf{end - 1};
	MAX_SIZE = 1E9; %Max number of bytes before conversion to sparse matrix
	if isempty(varargin)
		binsize = binunit;
	else
		binsize = varargin{1};
		if binsize < binunit
			error('Cannot have custom bin size less than asdf time bin.');
        end
    end
    nemp = find(cellfun(@isempty, asdf(1:end-2)));
    nemp = [setdiff(1:n_neu, nemp) n_neu+1 n_neu+2];    
    asdf = asdf(nemp);
    n_neu = length(nemp)-2;
    info(1) = n_neu;
    asdf{end} = info;
    
    minVal = min(cellfun(@min, asdf(1:end-2)))-binsize;
    
	% very simple check of validity of ASDF
	if n_neu ~= size(asdf,1) - 2
		error('Invalid n_neu information is contained in this ASDF');
	end

	% Get rid of unnecessary metadata
	asdf = asdf(1:(length(asdf)-2));

	% Convert the spike times to bin indices based on the time unit for each bin
	% and the specified size of each bin in the resulting raster
	convToBinInd = @(x) ceil(nonzeros(x-minVal)' ./ binsize);
	asdf = cellfun(convToBinInd, asdf, 'UniformOutput', 0);
    if binsize > binunit
		numReps = @(x, y) diff([x; length(y)]);
		[J, V, ~] = cellfun(@unique, asdf, 'UniformOutput', 0);
        V = cellfun(numReps, V, asdf, 'UniformOutput', 0);
        V = vertcat(V{:})';
		spks = cellfun('length', J);
		J = horzcat(J{:});
	else 
        spks = cellfun('length', asdf);
		J = horzcat(asdf{:});
		V = ones(size(J));		
    end
    
	% Increment each value greater than the cumulative sum of the length of each
	% neuron's spk times array to get our "I" indexes in the raster
	I = ones(size(J));
	spks = cumsum(spks);
    for i = 1:(length(spks)-1)
        I((spks(i)+1):spks(i+1)) = i+1;
    end
	cols = 1 + (binunit * duration / binsize);
    cols = uint32(ceil(cols));
	if (n_neu * cols * 4) < MAX_SIZE
		raster = zeros(n_neu, cols, 'uint32');
		raster(sub2ind([n_neu, cols], I, J)) = V;
    else
		raster = sparse(I,J,V, n_neu, double(cols));	
	end
	toc;
end
