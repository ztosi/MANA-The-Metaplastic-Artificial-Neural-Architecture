%  grp = zeros(10000000,6, 'uint16');
%  parfor i = 1:10000000
%      grp(i,:) = randperm(780, 6); 
% figure end
disp('done');
nullNumConC6 = zeros(50,31);

k = 1;
for i = 1:50
    %wte = exwts{i}~=0;
    wte = dir_generate_srand(wtEE~=0);
    for j = 1:10000000
        if mod(k, 10000) == 0
            wte = dir_generate_srand(wtEE~= 0);
            disp([num2str(k/10000) ' ' num2str(i)]);
        end
        index = nnz(wte(grp(k,:),grp(k,:)))+1;
        k = mod(k, 10000000);
        nullNumConC6(i, index) = nullNumConC6(i, index)+1;
        k = k+1;
    end
end