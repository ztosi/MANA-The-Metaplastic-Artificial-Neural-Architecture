cd '/home/zach/Desktop/final'
%close all;
NUM_SETS = 9;
NUM_PAT = 5;
TOT = NUM_SETS * NUM_PAT;
NUM_NEU = 540;
NUM_EX = 432;
NUM_INP = 100;
allWts = zeros(NUM_NEU, NUM_NEU, TOT);
inpMats = zeros(NUM_INP, NUM_NEU, TOT);
exMats = zeros(NUM_EX, NUM_EX, TOT);
allWts10 = zeros(NUM_NEU,NUM_NEU, TOT);
frs = zeros(TOT, NUM_NEU);
pfrs = zeros(TOT, NUM_NEU);
mfrs = zeros(TOT, NUM_NEU);
ths = zeros(TOT, NUM_NEU);
mot3 = zeros(TOT, 13);
%vers = zeros(TOT, NUM_NEU);
%stdVer = zeros(TOT, NUM_NEU);
%nullMV = zeros(TOT, NUM_NEU);
%nullStVL = zeros(TOT, NUM_NEU);
%nullStVU = zeros(TOT, NUM_NEU);
exin = zeros(TOT, NUM_NEU);
wtValsEx = cell(TOT, 1);
pei = zeros(TOT, 5);
pN = zeros(TOT, NUM_NEU);
modMean = zeros(3, NUM_NEU, TOT);
nullM = zeros(3, NUM_NEU, TOT);
stdnL = zeros(3, NUM_NEU, TOT);
stdnU = zeros(3, NUM_NEU, TOT);
kIn = zeros(TOT, NUM_NEU);
kOut = zeros(TOT, NUM_NEU);
kd = zeros(TOT, NUM_NEU);
kIne = zeros(TOT, NUM_EX);
kOute = zeros(TOT, NUM_EX);
kde = zeros(TOT, NUM_EX);
kIn10 = zeros(TOT, NUM_NEU);
kOut10 = zeros(TOT, NUM_NEU);
kd10 = zeros(TOT, NUM_NEU);
cumData = zeros(TOT, 5);
ccd = cell(TOT,4);
% cumCurveAll = cell(TOT, 1);
% cumCurveInc = zeros(TOT, NUM_NEU);
% cumCurveOut = zeros(TOT, NUM_NEU);
% cumCurveIncNull1 = zeros(TOT, NUM_NEU);
% cumCurveOutNull1 = zeros(TOT, NUM_NEU);
% cumCurveIncNull2 = zeros(TOT, NUM_NEU);
% cumCurveOutNull2 = zeros(TOT, NUM_NEU);

k = 1;
for j = 1:NUM_PAT
    for i = 1:NUM_SETS
        dname = ['./', num2str(j) ,'_HPNN_Gramm/Outputs/', num2str(i), '/'];
        load([dname, '4900000.0LAIP_FiringRates.mat']);
        load([dname, 'InAndDelay.mat']);
        load([dname, '4900000.0PrefFRs.mat']);
        load([dname, '4900000.0Thresholds.mat']);
        load([dname, '4900000.0LAIPWtMatCheckIn.mat']);
        disp(k);
        ei = eiArr == 1;
        allWts(:,:,k) = wtMat;
        inpMats(:,:,k) = inMat;
        exMats(:,:,k) = wtMat(ei,ei);
        frs(k, :) = FiringRates;
        pfrs(k,:) = PrefFRs;
        mfrs(k,:) = mean(FRs(2:4900, :));
        ths(k,:) = Thresholds;
        [kIn(k,:), kOut(k,:), kd(k,:)] = nodeDegrees(wtMat);
        wv = sort(abs(nonzeros(wtMat)), 'descend');
        cutoff = wv(uint32(0.1 * length(wv)));
        wtsTop10 = wtMat .* (abs(wtMat) > (cutoff));
        allWts10(:,:,k) = wtsTop10;
        [kIn10(k,:), kOut10(k,:), kd10(k,:)] = nodeDegrees(wtsTop10);       
        [pei(k,1), pei(k, 2), pei(k, 3), pei(k, 4), pei(k,5)] = ...
            eiratios(wtMat, ei);
        [kIne(k,:), kOute(k,:), kde(k,:)] = nodeDegrees(wtMat(ei,ei));
        exin(k, :) = ei;
 
      
        [ccd{k,1}, ccd{k,2}, ccd{k,3}, ccd{k,4}, ...
            cumData(k,1), cumData(k,2), cumData(k,3), cumData(k,4), ...
            cumData(k,4)] = cumsum50(wtMat(ei,ei));
%         cumCurveInc(k,:) = cumSum(sort(sum(abs(wtMat)), 'descend')) ...
%             ./ sum(sum(abs(wtMat)));
%         cumCurveOut(k,:) = cumSum(sort(sum(abs(wtMat),2), 'descend')) ...
%             ./ sum(sum(abs(wtMat)));
%         linind = abs(wtMat(:));
%         [I, ~, V] = find(linind);
%         null1 = zeros(1, length(linind));
%         null1(randperm(length(null1), length(V))) = V;
%         null1 = reshape(null1, NUM_NEU, NUM_NEU);
%         null2 = zeros(1, length(linind));
%         null2(I) = V(randperm(length(V)));
%         null2 = reshape(null2, NUM_NEU, NUM_NEU);
%         
%         cumCurveIncNull1(k,:) = cumSum(sort(sum(abs(null1)), 'descend')) ...
%             ./ sum(sum(abs(null1)));
%         cumCurveOutNull1(k,:) = cumSum(sort(sum(abs(null1),2), 'descend')) ...
%             ./ sum(sum(abs(null1)));    
%         cumCurveIncNull2(k,:) = cumSum(sort(sum(abs(null2)), 'descend')) ...
%             ./ sum(sum(abs(null2)));
%         cumCurveOutNull2(k,:) = cumSum(sort(sum(abs(null2),2), 'descend')) ...
%             ./ sum(sum(abs(null2)));
%         
%         cumCurveAll{k} = sort(V, 'descend');
    
        close all;
        k = k+1;
    end
end

save(['NetworkData_' datestr(datetime)]);

L = k;
k=1;
for k = 1: L
    wtMat = allWts(:,:,k);
    ei = exin(k);
    [p, q, r, s, t] ...
        = commonNeigh(wtMat(ei, ei)~= 0);
    size(p)
    pN(k,1:nume) = p(1:nume);
    modMean(1:3,1:nume,k) = q;
    nullM(1:3,1:nume,k) = r;
    stdnL(1:3,1:nume,k) = s;
    stdnU(1:3,1:nume,k) = t;
    mot3(k, :) = songMotifs(wtMat(ei, ei) ~= 0)';
    
end

findGroupCons;

exin = exin==1;

