cd '/home/zach/Desktop/FinalOuts'
%close all;
NUM_SETS = 10;
NUM_PAT = 5;
TOT = NUM_SETS * NUM_PAT;
NUM_NEU = 1000;
allWts = zeros(NUM_NEU, NUM_NEU, TOT);
allWts10 = zeros(NUM_NEU,NUM_NEU, TOT);
frs = zeros(TOT, NUM_NEU);
ths = zeros(TOT, NUM_NEU);
mot3 = zeros(TOT, 13);
vers = zeros(TOT, NUM_NEU);
stdVer = zeros(TOT, NUM_NEU);
nullMV = zeros(TOT, NUM_NEU);
nullStVL = zeros(TOT, NUM_NEU);
nullStVU = zeros(TOT, NUM_NEU);
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
kd10 = zeros(TOT, NUM_NEU);
kIn10 = zeros(TOT, NUM_NEU);
kOut10 = zeros(TOT, NUM_NEU);
kd10 = zeros(TOT, NUM_NEU);

k = 1;
for j = 1:NUM_PAT
    for i = 1:NUM_SETS
        dname = ['./Out', num2str(j), '/', num2str(i),'/'];
        load([dname, 'LAIP_FiringRates.mat']);
        %load([dname, 'LAIP_PrefFRs.mat']);
        %load([dname, 'LAIP_Thresholds.mat']);
        load([dname, 'LAIPTwMat.mat']);
        disp(k);
        allWts(:,:,k) = wts;
        frs(k, :) = FiringRates;
        %ths(k,:) = thresholds(5000, :);
        [kIn(k,:), kOut(k,:), kd(k,:)] = nodeDegrees(wts);
        wv = sort(abs(nonzeros(wts)), 'descend');
        cutoff = wv(uint32(0.1 * length(wv)));
        wtsTop10 = wts .* (abs(wts) > (cutoff));
        allWts10(:,:,k) = wtsTop10;
        [kIn10(k,:), kOut10(k,:), kd10(k,:)] = nodeDegrees(wtsTop10);
        
        [pei(k,1), pei(k, 2), pei(k, 3), pei(k, 4), pei(k,5)] = eiratios(wts);
        %[ vers(k, :), stdVer(k, :), nullMV(k, :), nullStVL(k, :), ...
        %    nullStVU(i, :) ]= StatSigVers(wts, kd, 1);
        %allRichClubs(wts);
        ei = (~any(wts<0,2) | (sum(wts,2)==0))';
        wtValsEx{k} = nonzeros(wts(ei, ei))';
        exin(k, :) = ei;
        %neighborHist(wts(ei, ei));
        %mot3(k, :) = songMotifs(wts(ei, ei) ~= 0)';
        nume = sum(ei)
        %[p, q, r, s, t] ...
        %    = commonNeigh(wts(ei, ei)~= 0);
        %size(p)
        %pN(k,1:nume) = p(1:nume);
        %modMean(1:3,1:nume,k) = q;
        %nullM(1:3,1:nume,k) = r;
        %stdnL(1:3,1:nume,k) = s;
        %stdnU(1:3,1:nume,k) = t;
        %close all;
        k = k+1;
    end
end
exin = exin==1;
%save('CommonNeighData');
