function [ rast, cRast ] = conv1SpkTrains( asdf, kern, varargin )
%   Convolves spike trains in an asdf file with an arbitrary kernel. Be
% careful, the output can take up a ton of memory...
tic
    if ~isrow(kern)
        kern = kern';
    end
    if ~isempty(varargin)
        tol = varargin{1};
    else
        tol = .001;
    end
    n_neu = asdf{end}(1);
    [rast, ~] = ASDFToRaster(asdf);
    [m,n]=size(rast);
    nWorkers = feature('numCores');
    taskR = cell(1,nWorkers);
    tpw_base = uint32(floor(m / nWorkers));
    tpw = ones(nWorkers,1, 'uint32') * tpw_base;
    rem = mod(m, nWorkers);
    tpw(1:rem) = tpw_base + 1;
    k = 1;
    for i = 1:nWorkers
        taskR{i} = {rast(k:(k+tpw(i)-1),:)};
        k = k+tpw(i);
    end
    
    function ctrain = convertConv(ra)
       [M,~] = size(ra);
       ctrain = cell(M,1);
       tic
       for kk = 1:M
          curr = ra(kk,:);
          curr = full(curr);
          curr = conv(full(curr), kern,'full');
          curr = curr(1:(end-(length(kern)-1)));
          ctrain{kk} = sparse(curr(curr>tol));
          clear curr;
       end
       clear ra;
       toc;
       disp('Task Done');
    end
    c=parcluster
    job = createJob(c);
    createTask(job, @convertConv, 1, taskR);
    submit(job);
    wait(job);
    outs = fetchOutputs(job);
    szev = [];
    for i = 1:nWorkers
       szev = [szev  cellfun('length', outs(i))']; 
    end
    sze = sum(szev);
    szev = cumsum(szev);
    V = zeros(1, sze);
    I = zeros(1, sze);
    J = zeros(1, sze);
    szev = [0, szev];
    k=1;
    for i = 1:nWorkers
        b = outs(i);
        b = b{1};
        for j = 1:tpw(i)          
            [ii, jj, ss] = find(b{j});
            V((szev(k)+1):szev(k+1)) = ss;
            I((szev(k)+1):szev(k+1)) = ii;
            J((szev(k)+1):szev(k+1)) = jj;
            k=k+1;
        end
    end
    cRast = sparse(I,J,V,m,n);
    toc
end

