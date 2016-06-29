function orderedInputs2SpkTrains( tokens, numNeu, ...
    tokenDur, ts, meanFr, jitSD, filePrefix )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

templates = numel(unique(tokens));
noPats = numel(tokens);

patterns = rand(uint32(tokenDur/ts), templates, numNeu);
patterns = patterns < (ts*meanFr/1000);
patternKey = tokens;
asdf = cell(numNeu+2, 1);
asdf{numNeu+1} = ts;
asdf{numNeu+2} = [numNeu, noPats*tokenDur/ts];
parfor j = 1:numNeu
    spktrain = patterns(:, patternKey, j);
    spktrain = spktrain(:)';
    inds = int32(find(spktrain));
    jitter = int32(randn(1, length(inds)) * (jitSD/ts));
    swapInds = inds + jitter;
    swapInds(swapInds < 1) = 1;
    swapInds(swapInds > length(spktrain)) = length(spktrain);
    strainf = zeros(size(spktrain));
    strainf(swapInds) = spktrain(inds);
    strainf(inds) = spktrain(swapInds);
    asdf{j} = find(strainf)*ts;
end

save([filePrefix '.mat'], 'asdf', 'patternKey', 'patterns');
    
end

