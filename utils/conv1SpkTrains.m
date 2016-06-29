function [ rast, cRast ] = conv1SpkTrains( asdf, kern, varargin )
%   Convolves spike trains in an asdf file with an arbitrary kernel. Be
% careful, the output can take up a ton of memory... Set the tolerance to
% make all values below that zero (2nd varargin), it helps save memory. The
% 1st optional argument is how you want the asdf binned... the larget the
% bin the less memory. It's your responsibility to make sure that the
% kernel matches the time bin.
tic;
    narginchk(1,3);
    if ~isrow(kern)
        kern = kern';
    end
    if ~isempty(varargin)
        if length(varargin) >= 1
            bin = varargin{1};
        end
        if length(varargin) == 2
            tol = varargin{2};
        end
    else
        tol = .001;
    end
    n_neu = asdf{end}(1);
    [rast, ~] = ASDFToRaster(asdf,bin);
    [m,n]=size(rast);
    cRastc = cell(m, 1);
    for i = 1:n_neu
        disp(i);
        q = conv(full(double(rast(i,:))), kern, 'full');
        q = q(1:(end-(length(kern)-1)));
        cRastc{i} = sparse(q .* (q>tol));
        clear q;
    end
    szev = cellfun(@nnz, cRastc);
    sze = sum(szev);
    szev = cumsum(szev);
    V = zeros(1, sze);
    I = zeros(1, sze);
    J = zeros(1, sze);
    szev = [0, szev'];
    for i = 1:n_neu
        [~, jj, ss] = find(cRastc{i});
        V((szev(i)+1):szev(i+1)) = ss;
        I((szev(i)+1):szev(i+1)) = i;
        J((szev(i)+1):szev(i+1)) = jj;
    end
    cRast = sparse(I,J,V,m,n);
    toc;
end

