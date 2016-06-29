function [ mn, usd, lsd, sd, mnNullmns, usdNull, lsdNull, sig ] ...
    = neighborStats( adjMats, vecs, nullMods, varargin)
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here

narginchk(3,7);

arrange = vecs;
group = ones(size(vecs), 'uint8');

for i=1:2:length(varargin)
    switch varargin{i}
        case 'ArrangeBy'
            arrange = varargin{i+1};
        case 'GroupWith'
            group = varargin{i+1};
        otherwise
            error('Unknown input.');
    end
end

adjMats = adjMats ~= 0;
[m, n, sets] = size(adjMats);
[r, c] = size(vecs);

generate = isscalar(nullMods);



if ~generate
    [nm, nn, nnull, nsets] = size(nullMods);
    if ~isequal([m, n, sets], [nm, nn, nsets])
        error('Null models were pre-rpovided, but dimensions do not match.');
    end
else
    nnull = nullMods;
end

if m~=n
    error('Square matrices only');
end

if r==m
    if c~= sets
        error('Test vectors must be paird one to one with test matrices');
    end
    vecs = vecs';
elseif c == m
    if r ~= sets
        error('Test vectors must be paird one to one with test matrices');
    end
else
    error('Test vector/matrix mismatch');
end
[r, c] = size(vecs);

mn = zeros(r, c, 2);
usd = zeros(r, c, 2);
lsd = zeros(r, c, 2);
sd = zeros(r, c, 2);
sig = zeros(r, c, 2);
mnNullmns = zeros(r, c, 2);
usdNull = zeros(r, c, 2);
lsdNull = zeros(r, c, 2);

for i = 1:sets
    inMat = bsxfun(@times, adjMats(:,:,i), vecs(i,:)');
    outMat = bsxfun(@times, adjMats(:,:,i), vecs(i,:))';
    [mn(i, :, 1), usd(i, :, 1), lsd(i, :, 1),sd(i, :, 1)] ...
        = statsNZ(inMat);
    [mn(i, :, 2), usd(i, :, 2), lsd(i, :, 2),sd(i, :, 2)] ...
        = statsNZ(outMat);
    mnmI = zeros(nnull, n);
    mnmO = zeros(nnull, n);
    %        for j = 1:length(kd)
    %            tic;
    %            for k=1:10000
    %               mnmI(k,j) = mean(vecs(i, randperm(n, kIn(j))));
    %               mnmO(k,j) = mean(vecs(i, randperm(n, kOut(j))));
    %            end
    %            toc;
    %        end
    vLoc = vecs(i,:);
    vLocT = vecs(i,:)';
    adjLoc = adjMats(:,:,i);
    
    if generate==1
        parfor j = 1:nnull
            RW = dir_generate_srand(adjLoc)~=0;
            inMat = bsxfun(@times, RW, vLocT);
            outMat = bsxfun(@times, RW, vLoc)';
            mnmI(j,:) = meanNZ(inMat);
            mnmO(j,:) = meanNZ(outMat);
        end
    else
        parfor j = 1:nnull 
            RW = nullMods(:,:,j, i)~=0;
            inMat = bsxfun(@times, RW, vLocT);
            outMat = bsxfun(@times, RW, vLoc)';
            mnmI(j,:) = meanNZ(inMat);
            mnmO(j,:) = meanNZ(outMat);
        end
    end
    
    mnNullmns(i, :, 1) = mean(mnmI);
    mnNullmns(i, :, 2) = mean(mnmO);
    [lsdNull(i, :, 1), usdNull(i, :, 1)] = semistd(mnmI);
    [lsdNull(i, :, 2), usdNull(i, :, 2)] = semistd(mnmO);
    les = mn(i, :, 1) < mnNullmns(i, :, 1);
    %        sig(i, les, 1) =  sum(bsxfun(@lt, mnmI(:, les), mn(i, les, 1))) ...
    %            ./ 10000;
    %        sig(i, ~les, 1) =  sum(bsxfun(@gt, mnmI(:, ~les), mn(i, ~les, 1))) ...
    %            ./ 10000;
    sig(i, les, 1) = (mnNullmns(i,les,1) - mn(i, les, 1)) ...
        ./ lsdNull(i,les,1);
    sig(i, ~les, 1) = (mn(i, ~les, 1) - mnNullmns(i,~les,1)) ...
        ./ usdNull(i,~les,1);
    sig(i, :, 1) = normcdf(-abs(sig(i,:,1)), 0, 1);
    les = mn(i, :, 2) < mnNullmns(i, :, 2);
    sig(i, les, 2) = (mnNullmns(i,les,2) - mn(i, les, 2)) ...
        ./ lsdNull(i,les,2);
    sig(i, ~les, 2) = (mn(i, ~les, 2) - mnNullmns(i,~les,2)) ...
        ./ usdNull(i,~les,2);
    sig(i, :, 2) = normcdf(-abs(sig(i,:,2)), 0, 1);
    %        sig(i, les, 2) =  sum(bsxfun(@lt, mnmI(:, les), mn(i, les, 2))) ...
    %            ./ 10000;
    %        sig(i, ~les, 2) =  sum(bsxfun(@gt, mnmI(:, ~les), mn(i, ~les, 2))) ...
    %            ./ 10000;
    
end

defCol = [0    0.4470    0.7410
    0.8500    0.3250    0.0980
    0.9290    0.6940    0.1250
    0.4940    0.1840    0.5560
    0.4660    0.6740    0.1880
    0.3010    0.7450    0.9330
    0.6350    0.0780    0.1840];
defCol = [defCol; 0.9*defCol];

figure;
colormap(flipud(parula));

subplot(121);
hold on
group = group(:);
noGroups = numel(unique(group));
ugroup = unique(group);
mnTemp = mnNullmns(:,:,1);
slTemp = lsdNull(:,:,1);
suTemp = usdNull(:,:,1);
mnTemp = mnTemp(:);
arrange = arrange(:);
slTemp = slTemp(:);
suTemp = suTemp(:);
for j = 1:noGroups
    i = ugroup(j);
    errbar(arrange(group==i), mnTemp(group==i), slTemp(group==i), ...
        suTemp(group==i), 'k-', 'LineWidth', 2);
    scatter(arrange(group==i), mnTemp(group==i), 40,  defCol(mod(i,14),:), 'filled', ...
        'MarkerEdgeColor', [0 0 0], 'LineWidth', 3);
end
mnTemp = mn(:,:,1);
sigTemp = sig(:,:,1);
sigTemp(sigTemp> 0.01) = 0.01;
mnTemp = mnTemp(:);
sigTemp = sigTemp(:);
scatter(arrange, mnTemp, 90, sigTemp, 'filled', ...
    'MarkerFaceAlpha', 0.85);
for j = 1:noGroups
    i = ugroup(j);
    scatter(arrange(group==i), mnTemp(group==i), 25, ...
        defCol(mod(i,14), :), 'filled', 'MarkerFaceAlpha', 0.85);
end
hold off;


subplot(122);
hold on;
mnTemp = mnNullmns(:,:,2);
slTemp = lsdNull(:,:,2);
suTemp = usdNull(:,:,2);
for j = 1:noGroups
    i = ugroup(j);
    errbar(arrange(group==i), mnTemp(group==i), slTemp(group==i), ...
        suTemp(group==i), 'k-', 'LineWidth', 2);
    scatter(arrange(group==i), mnTemp(group==i), 40,  defCol(mod(i,14),:), 'filled', ...
        'MarkerEdgeColor', [0 0 0], 'LineWidth', 3);
end
mnTemp = mn(:,:,2);
sigTemp = sig(:,:,2);
sigTemp(sigTemp>0.01) = 0.01;
mnTemp = mnTemp(:);
sigTemp = sigTemp(:);
scatter(arrange, mnTemp, 90, sigTemp, 'filled', ...
    'MarkerFaceAlpha', 0.85);
for j = 1:noGroups
    i = ugroup(j);
    scatter(arrange(group==i), mnTemp(group==i), 25, ...
        defCol(mod(i,14), :), 'filled', 'MarkerFaceAlpha', 0.85);
end

hold off;

end

