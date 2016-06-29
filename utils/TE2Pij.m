function [transmissionProbability] = TE2Pij(transferEntropy, firingRate)
% transmissionProbability = transferEntropyToTransmissionProbability(transferEntropy, firingRate)
%
%    transferEntropy - (n,n) Table of transfer entropy of n neurons.
%    firingRate - (n,1) Firing rate of neurons. (firing probability in one bin)
%
% Returns:
%    transmissionProbability - (n,n) Table of transmission probability of n neurons.
%
%
% Description :
%    This is a converter from transfer entropy to transmission probability.
%    TE and TP has exact mapping.
%
%
% Example :
%    
%
% Author   : Shinya Ito
%            Indiana University
%            Zach Tosi
%            Indiana University
%
% Last modified on 2/12/2016 - code was vectorized

% transferEntropy = sig_TE;
% firingRate = SpontRates;
%firingRate = cellfun(@numel,asdf_raw(1:asdf_raw{end}(1)))/asdf_raw{end}(2);

% validity check
narginchk(2,2);
if ~isvector(firingRate)
    error('Firing Rate (p of spiking in a given time bin) input must be a vector.')
end
te1 = size(transferEntropy,1);
te2 = size(transferEntropy,2);
fr = numel(firingRate);
if te1~=te2
	error('Transfer entropy must be square matrix');
elseif te1~=fr
	error('Different number of neurons between transfer entropy and firing rate.');
end

if isrow(firingRate)
    firingRate = firingRate';
end

tic;
transmissionProbability = zeros(size(transferEntropy));
[s, t, ~] = find(transferEntropy~=0);
% Using repmat instead of bsxfun... inital cost of repmats smaller than
% repeated singleton expansion which would happen almost a dozen times
% in the TEtable calculation...
b = repmat(0:0.001:.999, length(s), 1);
fi = repmat(firingRate(t), 1, 1000);
fj = repmat(firingRate(s), 1, 1000);
b = fi - (fj .* b);
c = repmat(0:0.001:.999, length(s), 1);
bb= 1-b;
bcb = 1-(b+c);
TEtable =  (1-fj).*bb.*log2(bb./(1-fi)) + fj.*bcb.*log2(bcb./(1-fi)) ...
    + (1-fj).*b.*log2(b./fi) + fj.*(b+c).*log2((b+c)./fi);
availInd = ~isnan(TEtable) & imag(TEtable)==0; % find valid indices
for i=1:length(s)
    if sum(availInd(i,:)) < 2
        continue;
    end
    transmissionProbability(s(i),t(i)) = interp1(TEtable(i,availInd(i,:)), ...
        c(i, availInd(i,:)), transferEntropy(s(i), t(i)));
end
% Remove NaNs
transmissionProbability(isnan(transmissionProbability)) = 0;
toc;

end
