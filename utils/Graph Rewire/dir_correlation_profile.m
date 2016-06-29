function [R_12,Z_12,n_12,nr_12,nsr_12]=dir_correlation_profile(s1,Nstat,edge_bins1,edge_bins2);
%Syntax:
%[R_12,Z_12,n_12,nr_12, nsr_12]=sym_corr_profile(s1,Nstat,edge_bins);
% INPUT:
% srand=sym_generate_srand(s1)
% s1 - the adjacency matrix of an undirected network  
% Nstat - (optional) the number of randomized networks in the ensemble. Default: 3
% edge_bins - (otional) the array to bin degrees. Default: [1,3,10,30,100...]
% OUTPUT:
% n_12 - number of edges connecting different bins to each other
% nr_12 - same averaged over Nstat realizations of a randomized network
% nsr_12 - sqaure of nr_12 averaged over Nstat realizations of a randomized network
% R_12 - correlation profile ratio: R_12=n_12./nr_12;
% Z_12 - correlation profile Z-score: Z_12=(n_12-nr_12)./sqrt(nsr_12-nr_12.^2);
% 
% Author: Sergei Maslov, ret from: http://www.cmth.bnl.gov/~maslov/dir_correlation_profile.m

srand=sign(abs(s1-diag(diag(s1))));

if (nargin < 2) Nstat=3; end;

k1=full(sum(srand'));
k1_max=max(k1);

k2=full(sum(srand));
k2_max=max(k2);

if (nargin < 3)
    edge_bins1(1)=1;
    edge_bins1(2)=3;
    m1=2;
    while edge_bins1(m1)<=k2_max;
        edge_bins1(m1+1)=10.*edge_bins1(m1-1); 
        m1=m1+1;
    end;    
end;

bedg1=edge_bins1;

if (nargin < 4)
    edge_bins2(1)=1;
    edge_bins2(2)=3;
    m1=2;
    while edge_bins2(m1)<=k2_max;
        edge_bins2(m1+1)=10.*edge_bins2(m1-1); 
        m1=m1+1;
    end;    
end;

bedg2=edge_bins2;
    

if k1_max>bedg1(end)
   bedg1(end+1)=k1_max;
end;

if k2_max>bedg2(end)
   bedg2(end+1)=k2_max;
end;


n_1_2_sym_orig=binned_srand_internal(srand,bedg1,bedg2);

disp(strcat('randomized network #',num2str(1)));
srand=dir_generate_srand(srand);
n_1_2=binned_srand_internal(srand,bedg1,bedg2);

aver_n_1_2_sym=n_1_2;
aver_sq_n_1_2_sym=n_1_2.^2;

for k=2:Nstat;
   disp(strcat('randomized network #',num2str(k)));
   srand=dir_generate_srand(srand);
   n_1_2=binned_srand_internal(srand,bedg1,bedg2);
   aver_n_1_2_sym=aver_n_1_2_sym+n_1_2;
   aver_sq_n_1_2_sym=aver_sq_n_1_2_sym+n_1_2.^2;
end;
aver_n_1_2_sym=aver_n_1_2_sym./Nstat;
aver_sq_n_1_2_sym=aver_sq_n_1_2_sym./Nstat;
err_n_1_2_sym=sqrt(aver_sq_n_1_2_sym-aver_n_1_2_sym.^2);


sym_ratio_1_2_sym=sym_ratio_1_2_sym(1:(end-1),1:(end-1));
dev_n_1_2_sym_orig=dev_n_1_2_sym_orig(1:(end-1),1:(end-1));


R_12=sym_ratio_1_2_sym;
Z_12=dev_n_1_2_sym_orig;
n_12=n_1_2_sym_orig(1:(end-1),1:(end-1));
nr_12=aver_n_1_2_sym(1:(end-1),1:(end-1));
nsr_12=aver_sq_n_1_2_sym(1:(end-1),1:(end-1));

sym_ratio_1_2_sym(end+1,:)=sym_ratio_1_2_sym(end,:);
sym_ratio_1_2_sym(:,end+1)=sym_ratio_1_2_sym(:,end);
dev_n_1_2_sym_orig(end+1,:)=dev_n_1_2_sym_orig(end,:);
dev_n_1_2_sym_orig(:,end+1)=dev_n_1_2_sym_orig(:,end);


figure;  pcolor(bedg1,bedg2,sym_ratio_1_2_sym); colorbar
set(gca,'Xscale','log','Yscale','log',...
    'Xtick',bedg1,'Ytick',bedg2,'Tickdir','out','Box','off');
shading flat; 
xlabel('K_1');
ylabel('K_2');
%title(['R(K_1,K_2) for network w/ ' num2str(size(srand,1)) ' nodes, ' num2str(full(sum(sum(srand))./2)) ' links']);
title('R(K_1,K_2)'); 

figure;  pcolor(bedg1,bedg2,dev_n_1_2_sym_orig); colorbar;
set(gca,'Xscale','log','Yscale','log',...
    'Xtick',bedg1,'Ytick',bedg2,'Tickdir','out','Box','off');
shading flat; 
xlabel('K_1');
ylabel('K_2');
%title(['Z(K_1,K_2) for network w/ ' num2str(size(srand,1)) ' nodes, ' num2str(full(sum(sum(srand))./2)) ' links']);
title('Z(K_1,K_2)'); 


% num2str(sym_ratio_1_2_sym)
% num2str(dev_n_1_2_sym_orig)


function n_1_2=binned_srand_internal(sa,bedg1,bedg2);
%n12=binned_srand_bedge(srand,bedge1,bedge2);

sa=sa-diag(diag(sa));
nb1=length(bedg1);
nb2=length(bedg2);
%sa=srand;
k_out_a=full(sum(sa'));
k_in_a=full(sum(sa));

k_1=k_out_a;
k_2=k_in_a;


[i1,j1]=find(sa);
E=length(i1);
n_1_2=zeros(nb1,nb2);

for i=1:E;
    kc1=k_1(i1(i));
    kc2=k_2(j1(i));
    if kc1.*kc2>0;
        %b1=1+floor(bpd*log10(kc1));
        %b2=1+floor(bpd*log10(kc2));
        %n_1_2(b1,b2)=n_1_2(b1,b2)+v1(i);
        b1=min(find(((bedg1-kc1)>0)))-1;
        b2=min(find(((bedg2-kc2)>0)))-1;
        n_1_2(b1,b2)=n_1_2(b1,b2)+1;
    end;
end;
end
end
