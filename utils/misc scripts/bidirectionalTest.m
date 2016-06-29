function bidirectionalTest( wtMat, delays )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

    ei = ~(sum(wtMat, 2) < 0);
    wts = wtMat(ei, ei); % Excitatory Only
    dlys = delays(ei, ei);
    dlyVals = unique(dlys)';
    [~, numD] = size(dlyVals);
    [r, c] = size(wtMat);
    N = r * (c-1);
    biProps = zeros(3, numD-2);
    index = 1;
    for i = (numD-1):-1:2
        wtsDC = (wts .* (dlys < dlyVals(i))) ~= 0;
        p = sum(sum(wtsDC)) / N;
        biProps(1, index) = (N/2) * p * p;
        biProps(2, index) = sum(sum(wtsDC .* wtsDC'))/2;
        biProps(3, index) = biProps(2, index) / biProps(1, index);
        index = index + 1;
    end
    biProps(3, :)
    dlyVals = fliplr(dlyVals(2:1:numD-1));
    figure; hold;
    size(dlyVals)
    size(biProps)
    n = numel(dlyVals);
    plot(dlyVals(1:n-2)./2, biProps(1, 1:n-2), 'b.-');
    plotyy(dlyVals(1:n-2)./2, biProps(2, 1:n-2), dlyVals(1:n-2)./2, biProps(3, 1:n-2));
    hold;

end

