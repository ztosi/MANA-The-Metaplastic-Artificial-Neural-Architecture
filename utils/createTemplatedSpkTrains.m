function createTemplatedSpkTrains( templates, numNeu, ...
    tempDur, ts, noPats, meanFr, jitSD, filePrefix, numFiles )
% Creates ASDF files wherein there are random orderings of jittered versions
% of some number of template spike trains
  
    for i = 1:numFiles
       patterns = rand(uint32(tempDur/ts), templates, numNeu);
       patterns = patterns < (ts*meanFr/1000);
       patternKey = randi(templates, 1, noPats);
       asdf = cell(numNeu+2, 1);
       asdf{numNeu+1} = ts;
       asdf{numNeu+2} = [numNeu, noPats*tempDur/ts];
       for j = 1:numNeu
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
       save([filePrefix '_' num2str(i) '.mat'], 'asdf', 'patternKey', 'patterns');
    end
end

