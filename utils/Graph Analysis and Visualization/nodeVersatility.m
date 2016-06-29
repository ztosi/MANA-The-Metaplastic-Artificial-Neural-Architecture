function [ vers ]...
    = nodeVersatility( wtMat, versVal, show )
%UNTITLED5 Summary of this function goes here
%   Detailed explanation goes here
    len = length(wtMat);
    vers = zeros(1, len);
    for i = 1:len 
       out = wtMat(i, :) ~= 0;
       in = wtMat(:, i) ~= 0;
       diffs = abs(versVal(i) - nonzeros([versVal(out), versVal(in)]));
       vers(i) = std(diffs);
    end
    if show == 1
        figure;
        scatter(versVal, vers);
    end
    %exDegs = nonzeros(degs .* ei);
    %exVers = nonzeros(vers .* ei);
    %scatter(exDegs, exVers, 'r');
    %hold;
    %inDegs = nonzeros(degs .* ~ei);
    %inVers = nonzeros(vers .* ~ei);
    %scatter(inDegs, inVers, 'b');
end

