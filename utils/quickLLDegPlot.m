function [ a, in, ou ] = quickLLDegPlot( k, kIn, kOut)
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here

figure;
[a,eda] = histcounts(k(:), 'BinMethod', 'integer', ...
    'Normalization', 'pdf');
[in,edin] = histcounts(kIn(:), 'BinMethod', 'integer', ...
    'Normalization', 'pdf');
[ou,edou] = histcounts(kOut(:), 'BinMethod', 'integer', ...
    'Normalization', 'pdf');

aLoc = zeros(size(k,1), length(eda)-1);
inLoc = zeros(size(kIn,1), length(edin)-1);
ouLoc = zeros(size(kOut,1), length(edou)-1);

for i=1:size(k,1)
    [aLoc(i,:),~] = histcounts(k(i,:), eda, ...
        'Normalization', 'pdf');
    [inLoc(i,:),~] = histcounts(kIn(i,:), edin, ...
        'Normalization', 'pdf');
    [ouLoc(i,:),~] = histcounts(kOut(i,:), edou, ...
        'Normalization', 'pdf');
end

[la, ua] = semistd(aLoc);
[lin,uin] = semistd(inLoc);
[lou,uou] = semistd(ouLoc);

nza = (a-la)>0;
nzin = (in-lin)>0;
nzou = (ou-lou)>0;

a = conv(a, normpdf(-3:3, 0, 1));
in = conv(in, normpdf(-3:3, 0, 1));
ou = conv(ou, normpdf(-3:3, 0, 1));

la = conv(la, normpdf(-3:3, 0, 1));
ua = conv(ua, normpdf(-3:3, 0, 1));
lin = conv(lin, normpdf(-3:3, 0, 1));
uin = conv(uin, normpdf(-3:3, 0, 1));
lou = conv(lou, normpdf(-3:3, 0, 1));
uou = conv(uou, normpdf(-3:3, 0, 1));


figure;
hold on;
bins = eda(1:end-1)+0.5;
h1=shadedErrorBar(bins(nza), a(nza), [ua(nza);la(nza)],...
    {'k', 'LineWidth', 2}, 0.5);
bins = edin(1:end-1)+1.5;
h2=shadedErrorBar(bins(nzin), in(nzin), [uin(nzin);lin(nzin)], ...
    {'r', 'LineWidth', 2}, 0.5);
bins = edou(1:end-1)+1.5;
h3=shadedErrorBar(bins(nzou), ou(nzou), [uou(nzou);lou(nzou)], ...
    {'b', 'LineWidth', 2}, 0.5);
leg=legend([h1.mainLine h2.mainLine h3.mainLine], ...
    'Degree', 'In-Degree', 'Out-Degree');
leg.Box = 'off';
set(gca, 'XScale', 'log');
set(gca, 'YScale', 'log');
hold off;

end

