tic;
wtMatO = wtMat; %Save original
wtMat = wtMat .* (abs(wtMat)>0.01);
[N, m] = size(wtMat);
nullMods = zeros(N,m,100, 'uint8');
nullModsee = zeros(sum(ei),sum(ei),100, 'uint8');
nullModsee10 = zeros(sum(ei),sum(ei),100, 'uint8');
wtValsEx = nonzeros(wtMat(ei,ei));
wtValsEx = sort(wtValsEx, 'descend');
cutoff10 = wtValsEx(uint32(length(wtValsEx)/10));
wtMEx10 = wtMat(ei,ei)>cutoff10;
wtMatee = wtMat(ei,ei);
disp('Generating null models...');
tic;
parfor i=1:100
   nullMods(:,:,i) = uint8(dir_generate_srand_bid_prev(wtMat~=0));
   nullModsee(:,:,i) = uint8(dir_generate_srand_bid_prev(wtMat(ei,ei)~=0));
   nullModsee10(:,:,i) = uint8(dir_generate_srand_bid_prev(wtMEx10));
end
toc;
disp('DONE');

[P, EE, EI, IE, II] = eiratios(wtMat, ei)
[sumNZA, meanNZA, stdNZA] = statsNZ(abs(wtMat));

[sumNZEE, meanNZEE, stdNZEE] = statsNZ(abs(wtMat(ei, ei)));
stdNumBins = N / 10 + 1;
wts = nonzeros(wtMat(ei,ei));
wtValsEx = 10*nonzeros(wts);
figure; hist(PrefFRs, stdNumBins); title('Pref. Firing Rates (spks/s)');
figure; hist(FiringRates, stdNumBins);title('Firing Rates (spks/s)');
figure; hist(nonzeros(wtValsEx), N); title('EPSPs');
%wtValsEx = wtValsEx(wtValsEx>.5);

%% Plot Weights
lgNrmFitAndFig(wtValsEx, 50, max(wtValsEx)/100, max(wtValsEx));


%% Plot Firing Rates
lgNrmFitAndFig(FiringRates, 50, min(FiringRates), max(FiringRates));
figure; [~, edge] = histcounts(FiringRates); ...
    histogram(FiringRates(ei), edge); hold;...
    histogram(FiringRates(~ei), edge); hold;


%% Mean, std, sum
[suIn, mIn, stIn] = statsNZ(wtMat);
[suOut, mOut, stOut] = statsNZ(wtMat');
%wtMatRw = dir_generate_srand(wtMat);
% suInRw = zeros(100, 1000);
% mInRw = zeros(1, 100);
% stInRw = zeros(1, 100);
% suOutRw = zeros(1, 100);
% mOutRw = zeros(1, 100);
% stOutRw = zeros(1, 100);
% for i = 1:100
%     [suInRw(i), mInRw(i), stInRw(i)] = statsNZ(nullMods(:,:,i));
%     [suOutRw(i), mOutRw(i), stOutRw(i)] = statsNZ(nullMods(:,:, i)');
% end


%% Degree
[kIn, kOut, k] = nodeDegrees(wtMat);
degPlot(wtMat);
degPlot(wtMat(ei, ei));

%% Versatility and Connectivity Stats
[ vers, stdVer, nullMV, nullStVL, nullStVU ]= StatSigVers(wtMat, k, nullMods, ...
    0.01, 1); % Degree Versatility
%[avgOIM, stdOIM] = sig_mean_std_out_of_ins(wtMat, nullMods, 0.01, ei, 1);
%[avgOIMee, stdOIMee] = sig_mean_std_out_of_ins(wtMat(ei, ei), nullModsee, ...
%    0.01, ones(1, sum(ei))>0, 1);
[ dynImp ] = dynamicalImportance( wtMat, vers, 1 );
allRichClubs(wtMat, wtMatee, wtMEx10, nullMods, nullModsee, nullModsee10);
%neighborHist(wtMat(ei, ei));

%% Motifs
biEx = sum(sum((wtMat(ei, ei) > 0) .* (wtMat(ei, ei)' > 0)));
N = sum(sum(wtMat(ei, ei) > 0));
p = N / (sum(ei) * (sum(ei) - 1));
biRatio = biEx / (N * p * p);
figure; hold on; title('3-Motifs All', 'FontSize', 18);
songMotifs(wtMat ~= 0);
hold off;
figure; hold on; title('3-Motifs Ex-Ex', 'FontSize', 18);
SM = songMotifs(wtMat(ei, ei) ~= 0);
hold off;


%% Other
%dynamicalImportance(wtMat, PrefFRs, 1);
%diversity(wtMat, PrefFRs, 1);
commonNeigh(wtMat(ei,ei), nullModsee, 1);
 toc;