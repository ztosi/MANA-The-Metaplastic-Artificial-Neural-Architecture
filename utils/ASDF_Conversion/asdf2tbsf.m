function [ tbsf ] = asdf2tbsf( asdf )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    tic
    meta = [asdf{length(asdf)-1} asdf{length(asdf)}];
    asdf = asdf(1:(length(asdf)-2));
    cores = feature('numCores');
    t_bins = cell(cores, 1);
    if isempty(gcp('nocreate'))
        loc_pool = parpool(cores);
    else
        loc_pool = gcp;
    end
    chunkSize = floor(length(asdf)/cores);
    for j = 1:cores
        start = ((j-1) * chunkSize) + 1;
        if j ~= cores
            finish = j * chunkSize;
        else
            finish = length(asdf);
        end
        f(j) = parfeval(loc_pool, @findUnique, 1, asdf(start:finish)); 
    end
    for j = 1:cores
       [~, val] = fetchNext(f);
       t_bins{j} = val;
    end
    t_bins = findUnique(t_bins);
    tbsf_pre = cell(length(t_bins), cores);
    parfor j = 1:cores
       start = ((j-1) * chunkSize) + 1;
        if j ~= cores
            finish = j * chunkSize;
        else
            finish = length(asdf);
        end
        loc = cell(length(t_bins), 1);
        for k = start:finish
            [~, ~, inds] = intersect(asdf{k}, t_bins);
            for l = 1:length(inds)
               loc{inds(l)} = [loc{inds(l)} k];
            end
        end
        tbsf_pre(:, j) = loc;
    end
    tbsf = cell(length(t_bins)+2, 1);
    parfor j = 1:length(t_bins)
       tbsf{j} = horzcat(tbsf_pre{j, :});
       tbsf{j} = uint16(tbsf{j});
    end
    tbsf{length(t_bins) + 1} = t_bins;
    tbsf{length(t_bins) + 2} = meta;
    toc
        
    function [thread_unique] = findUnique(asdf_loc)
        t_unique = asdf_loc{1};
        for i = 2:length(asdf_loc)
           t_unique = unique([t_unique asdf_loc{i}]); 
        end
        thread_unique = t_unique;
    end
    
end

