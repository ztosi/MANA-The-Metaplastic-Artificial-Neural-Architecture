ei = eiArr == 1;
eiArr = ei;

% rast = ASDFToRaster(spkTimes_byNeuron, 1);
% rast10 = ASDFToRaster(spkTimes_byNeuron, 10);
% Avalanche_detection

ttMods = Modules(cellfun(@length, Modules)>20);
msiz = cellfun(@length, ttMods);

numMods = length(Modules);
numSigMods = length(ttMods);

colors = rand(numMods, 3);
colors = bsxfun(@rdivide, exp(5*colors), sum(exp(5*colors),2));
colors = colors + 0.2;
colors = bsxfun(@rdivide, colors, max(colors));
colors = bsxfun(@rdivide, exp(3*colors), sum(exp(3*colors),2));
colors = colors + 0.2;
colors = bsxfun(@rdivide, colors, max(colors));
modIndexing = [];
modLocs = [];
for i=1:numMods
   modIndexing = [modIndexing Modules{i}]; 
   modLocs = [modLocs ones(1, length(Modules{i}))*i];
   [~, I] = min(colors(i,:));
   colors(i,I) = 0;
end

multi = histcounts(modIndexing, 'BinMethod', 'integers');
[uModins, ic, ia] = unique(modIndexing, 'stable');
umodLocs = modLocs(ic);

modsrtRast10 = rast10(uModins,:);
modsrtRast = rast(uModins,:);
[kIn, kOut, k] = nodeDegrees(wtMat);
toE = sum(wtMat(:, eiArr)~=0,2)';
toI = sum(wtMat(:, ~eiArr)~=0,2)';

modPvals = zeros(numSigMods, 4);

for i=1:numSigMods
   [~, modPvals(i,1)] = kstest2(PrefFRs, PrefFRs(Modules{i}));
   [~, modPvals(i,2)] = kstest2(kIn, kIn(Modules{i}));
   [~, modPvals(i,3)] = kstest2(kOut, kOut(Modules{i}));
   modPvals(i,4) = length(Modules{i});
end
