function [ out ] = meanNZ( mat, varargin )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    narginchk(1,2);
    if (isempty(varargin))
        dim = 1;
    else
        if isscalar(varargin{1})
            dim = varargin{1};
            if dim > ndims(mat)
                error('Requested dimension exceeds matrix dimensions');
            end
        else
            error ('Dimension input must be scalar.');
        end
    end
    
    out = sum(mat, dim) ./ sum(mat~=0, dim);

end

