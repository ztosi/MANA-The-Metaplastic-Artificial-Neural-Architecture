function [misi, ve, ve0, mvWin, spks, t, tas] = efrTest

t = 0:0.25:1000000;
spks = zeros(size(t));
spks(randperm(length(t), 5000)) = 1;
eps = zeros(size(t));
eps0 = zeros(size(t));
mvWin = zeros(size(t));
tas = zeros(size(t));
% for i = 2:length(spks)
% eps(i) = 1000/((i*0.25)-lastSpkTime);
% if spks(i)==1
% lastSpkTime = i*0.25;
% end
% end
lastSpkTime=-200;
misi=zeros(size(t));
k=1;
ve = zeros(size(eps));
ve0 = zeros(size(eps0));
winSz = 10000;
tauA=winSz;
for i = 2:length(spks)
eps(i) = eps(i-1) - 0.25 * (eps(i-1)/winSz) + ...
    spks(i-1);
%0.25*(1+(exp(lastSpkTime-(i-1))))*spks(i-1);
eps0(i) = eps0(i-1) - 0.25 *(eps0(i-1)/tauA) + spks(i-1);
ve(i) = ve(i-1) + (0.25*((eps(i-1)/winSz)-ve(i-1)));
ve0(i) = ve0(i-1) + (0.25*((eps0(i)/tauA)-ve0(i-1)));
if ve0(i) > 0
    tauA = 10000/sqrt(ve0(i)*1000);
end
tas(i) = tauA;
misi(i) = (spks(i-1)*(1/winSz)) + (misi(i-1)*(1-(1/winSz)));



% if spks(i)==1
%     %misi(i) = (((i-1)*0.25 - lastSpkTime)*(1/100)) + ...
%     %misi(k)*(1-1/100);
%     k = i;
%     lastSpkTime = (i-1)*0.25;
% end
end

parfor i=2:length(spks)
    spksLoc = spks;
    if i/4 <= winSz
        mvWin(i) = sum(spksLoc(1:i))/(i/4000);
    else
        mvWin(i) = sum(spksLoc(i-(4*winSz):i))/(i/4000);
    end
end

% for i = 2:length(eps)
% 
% end

ve = ve * 1000;
ve0 = ve0 * 1000;
misi = misi * 4000;

end