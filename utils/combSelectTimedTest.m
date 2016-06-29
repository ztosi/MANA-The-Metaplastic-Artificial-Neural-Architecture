tic;
bob = zeros(1000000, 6);
parfor i = 1:1000000
    bob(i,:) = combSelect(780, 6,p(i));
end
 toc;