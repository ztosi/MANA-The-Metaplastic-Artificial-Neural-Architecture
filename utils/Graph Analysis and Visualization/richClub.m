function [rccs] = richClub( adjMat )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
    adjMat = adjMat ~= 0;
    degs = sum(adjMat);
    maxDeg = max(degs);
    
    rccs = richclubcoeff(adjMat, 1:maxDeg);
    rccShuff = zeros(20, maxDeg);
    parfor i = 1:100
        rccShuff(i, :) = richclubcoeff(dir_generate_srand(adjMat),...
            1:maxDeg);
    end
    plot(rccs, 'r');   
    meanRccSh = mean(rccShuff);
    [stdRccShL, stdRccShU] = semistd(rccShuff);
    zers = (meanRccSh - stdRccShL) <= 0;
    stdRccShL(zers) = 1E-6; 
    hold; errorbar([1 int32(ceil(maxDeg/20)):int32(ceil(maxDeg/20)):maxDeg maxDeg], ...
        meanRccSh(1, [1 int32(ceil(maxDeg/20)):int32(ceil(maxDeg/20)):maxDeg maxDeg]), ...
         stdRccShL(1, [1 int32(ceil(maxDeg/20)):int32(ceil(maxDeg/20)):maxDeg maxDeg]), ...
         stdRccShU(1, [1 int32(ceil(maxDeg/20)):int32(ceil(maxDeg/20)):maxDeg maxDeg]));
     
    plot(rccs ./ meanRccSh, 'k');
    mx = [1:int32(ceil(maxDeg/20)):maxDeg maxDeg];
    plot(mx, ones(1, numel(mx)), '--k');hold;
end

