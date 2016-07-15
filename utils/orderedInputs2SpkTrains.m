function asdf =  orderedInputs2SpkTrains( tokens, numNeu, ...
    tokenDur, ts, meanFr, jitSD, filePrefix )
% Translates some pattern of tokens into spike trains with a slight jitter,
% designed to transform an arbitrary sequence of discrete inputs into spike
% trains suitable as input to a spiking neural network. EXAMPLE: You want
% to feed a RSNN the Elman Grammar dataset, which consists of distinct
% tokens like "Boys", "John", "Mary", "lives", "see" etc... To prepare an
% input like that for this function simply assign each distinct input token
% a number (Boy-> 1, Girl->2, etc). This function will create a number of
% spike trains (specified by the caller by "numNeu") commensurate with the
% desired number of input neurons and assign each a poisson distributed set
% of spike times with the desired mean firing rate. Each unique token (now
% each consisting of numNeu distinct random spike trains) then has a unique
% template set of spike trains assigned to them. This method then
% concatenates these spike trains in the provided order so as to mirror the
% ordering of the input tokens and jitteres each spike train by the desired
% jitSD, so as to make each instance of a token similar, but slightly
% different reflecting of noisy conditions. Outputs/saves the result in an
% ASDF file.

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
    asdf{j} = find(jitterSpkTrain(spktrain, uint32(jitSD/ts)))*ts;
end

save([filePrefix '.mat'], 'asdf', 'patternKey', 'patterns');
    
end

