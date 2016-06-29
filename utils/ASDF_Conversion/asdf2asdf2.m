function [ asdf2 ] = asdf2asdf2( asdf )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    ts = asdf{end-1};
    maxBins = uint32(asdf{end}(2));
    asdfCh = asdf(1:(end-2));
    neChan = ~cellfun('isempty',  asdfCh);
   
    asdfCh = cellfun(@(x) uint32(x./ts)+1, asdfCh(neChan), 'UniformOutput', 0);
    
    asdf2 = struct('raster', {asdfCh}, 'nchannels', nnz(neChan), 'nbins', maxBins, ...
        'binsize', ts);

end

