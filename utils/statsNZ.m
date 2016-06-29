function [ meanNZ, stdNZU, stdNZL, stdNZ ] = statsNZ( wtMat )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    wtMat(~isfinite(wtMat)) = 0;
    wtMat(isnan(wtMat)) = 0;
    
    adj = wtMat~=0;
    pops = sum(adj);
    meanNZ = sum(wtMat)./pops;
    temp = bsxfun(@minus, wtMat, meanNZ);
    temp = temp .* adj;
    stdNZU = temp .* (temp>0);
    stdNZL = temp .* (temp<0);
    stdNZU = sqrt(sum(stdNZU.^2) ./ (pops-1));
    stdNZL = sqrt(sum(stdNZL.^2) ./ (pops-1));
    stdNZ = sqrt(sum(temp.^2)./( pops-1));
    
end

