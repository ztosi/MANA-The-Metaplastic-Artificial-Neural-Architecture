function [ totSum, inSum, outSum, allsyns, t50, i50, o50, a50, N ] = cumsum50( wtMat )
%UNTITLED3 Summary of this function goes here
%   Detailed explanation goes here

    wtMat(isnan(wtMat)) = 0;
    wtMat = abs(wtMat);
    inSum = sum(wtMat);
    outSum = sum(wtMat, 2)';
    totSum = inSum + outSum;
    
    allsyns = sort(nonzeros(wtMat(:)), 'descend');
    [inSum, inI] = sort(inSum, 'descend');
    [outSum, outI] = sort(outSum, 'descend');
    [totSum, totI] = sort(totSum, 'descend');

    su = sum(inSum); % doesn't matter...
    
    inSum = cumsum(inSum)./su;
    outSum = cumsum(outSum)./su;
    totSum = cumsum(totSum)./(2*su);
    allsyns = cumsum(allsyns)./sum(allsyns);

    [~,t50] = min(abs(totSum-.5));
    [~,i50] = min(abs(inSum-.5));
    [~,o50] = min(abs(outSum-.5));
    [~,a50] = min(abs(allsyns-.5));

    N = size(wtMat,1);

end

