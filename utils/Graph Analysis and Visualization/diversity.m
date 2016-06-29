function [ inDiv, outDiv ] = diversity( wtMat, PrefFRs, show )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here

    [M, N] = size(wtMat);
    wtMat = abs(wtMat); %No negative elements allowed
    [kIn, kOut, ~] = nodeDegrees(wtMat);

    % Call volume is synaptic strength AND how often it's used
    %wtMat = bsxfun(@times, wtMat, PrefFRs');
    
    inNorm = bsxfun(@rdivide, wtMat, sum(wtMat)); % Normalize columns
    l2inn = log2(inNorm);
    l2inn(isnan(l2inn)) = 0;
    l2inn(~isfinite(l2inn)) = 0;
    inNorm = inNorm .* l2inn;
    suinn = -sum(inNorm);
    inDiv =  suinn ./ log2(kIn);
    inDiv(isnan(inDiv)) = 0;
 
    outNorm = bsxfun(@rdivide, wtMat, sum(wtMat,2)); % Normalize rows
    l2outn = log2(outNorm);
    l2outn(isnan(l2outn)) = 0;
    l2outn(~isfinite(l2outn)) = 0;
    outNorm = outNorm .* l2outn;
    suoutn = -sum(outNorm,2)';
    outDiv =  suoutn ./ log2(kOut);
    outDiv(isnan(outDiv)) = 0;

    if show == 1
       figure; hist(inDiv, N/10 + 1); title('In Diversity');
       figure; hist(outDiv, M/10 + 1); title('Out Diversity');
       figure; scatter(PrefFRs, inDiv); title('PFRs v. In Div');
       figure; scatter(PrefFRs, outDiv); title('PFRs v. Out Div');  
    end
    
end

